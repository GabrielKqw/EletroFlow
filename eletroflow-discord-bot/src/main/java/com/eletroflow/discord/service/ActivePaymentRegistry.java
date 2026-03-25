package com.eletroflow.discord.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActivePaymentRegistry {

    private final Path stateFile;
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final ConcurrentHashMap<String, TrackedPayment> trackedPayments;

    public ActivePaymentRegistry(Path stateFile) {
        this.stateFile = stateFile;
        this.trackedPayments = new ConcurrentHashMap<>(load());
    }

    public void put(TrackedPayment trackedPayment) {
        trackedPayments.put(trackedPayment.paymentId(), trackedPayment);
        persist();
    }

    public void remove(String paymentId) {
        trackedPayments.remove(paymentId);
        persist();
    }

    public Collection<TrackedPayment> all() {
        return trackedPayments.values();
    }

    private Map<String, TrackedPayment> load() {
        if (!Files.exists(stateFile)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(stateFile.toFile(), new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load bot state", exception);
        }
    }

    private void persist() {
        try {
            Files.createDirectories(stateFile.getParent());
            objectMapper.writeValue(stateFile.toFile(), trackedPayments);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist bot state", exception);
        }
    }

    public record TrackedPayment(
            String paymentId,
            String planKey,
            String discordUserId,
            String ticketChannelId
    ) {
    }
}
