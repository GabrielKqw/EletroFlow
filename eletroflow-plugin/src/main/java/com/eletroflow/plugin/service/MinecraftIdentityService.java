package com.eletroflow.plugin.service;

import com.eletroflow.plugin.config.MinecraftSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class MinecraftIdentityService {

    private final MinecraftSettings settings;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public MinecraftIdentityService(MinecraftSettings settings, ObjectMapper objectMapper) {
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public ResolvedMinecraftIdentity resolve(String minecraftUsername) {
        validateUsername(minecraftUsername);
        if (!settings.onlineMode()) {
            return new ResolvedMinecraftIdentity(
                    offlineUuid(minecraftUsername).toString(),
                    minecraftUsername
            );
        }
        try {
            String encodedUsername = URLEncoder.encode(minecraftUsername, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + encodedUsername))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204 || response.body() == null || response.body().isBlank()) {
                throw new IllegalArgumentException("Nick do Minecraft nao encontrado");
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Falha ao consultar a conta Minecraft");
            }
            JsonNode payload = objectMapper.readTree(response.body());
            String resolvedName = payload.path("name").asText("");
            String rawUuid = payload.path("id").asText("");
            if (resolvedName.isBlank() || rawUuid.isBlank()) {
                throw new IllegalStateException("Resposta invalida da conta Minecraft");
            }
            return new ResolvedMinecraftIdentity(
                    canonicalUuid(rawUuid).toString(),
                    resolvedName
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao resolver a conta Minecraft", exception);
        }
    }

    private void validateUsername(String minecraftUsername) {
        if (minecraftUsername == null || minecraftUsername.isBlank() || minecraftUsername.length() > 16) {
            throw new IllegalArgumentException("Nick do Minecraft invalido");
        }
    }

    private UUID offlineUuid(String minecraftUsername) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + minecraftUsername).getBytes(StandardCharsets.UTF_8));
    }

    private UUID canonicalUuid(String rawUuid) {
        String normalized = rawUuid.replace("-", "");
        if (normalized.length() != 32) {
            throw new IllegalStateException("UUID do Minecraft invalido");
        }
        String formatted = normalized.substring(0, 8) + "-"
                + normalized.substring(8, 12) + "-"
                + normalized.substring(12, 16) + "-"
                + normalized.substring(16, 20) + "-"
                + normalized.substring(20, 32);
        return UUID.fromString(formatted);
    }

    public record ResolvedMinecraftIdentity(
            String uuid,
            String username
    ) {
    }
}
