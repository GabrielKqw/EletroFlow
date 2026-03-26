package com.eletroflow.plugin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PluginConfigurationLoader {

    private final JavaPlugin plugin;
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    public PluginConfigurationLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public PluginSettings loadSettings() {
        FileConfiguration config = plugin.getConfig();
        return new PluginSettings(
                config.getString("server.id"),
                new DatabaseSettings(
                        config.getString("database.jdbc-url"),
                        config.getString("database.username"),
                        config.getString("database.password")
                ),
                new DiscordSettings(
                        config.getString("discord.token"),
                        config.getString("discord.guild-id"),
                        config.getString("discord.panel-channel-id"),
                        config.getString("discord.support-role-id"),
                        config.getLong("discord.payment-poll-interval-seconds", 20L)
                ),
                new EfiSettings(
                        config.getString("efi.base-url"),
                        config.getString("efi.client-id"),
                        config.getString("efi.client-secret"),
                        config.getString("efi.certificate-path"),
                        config.getString("efi.certificate-password"),
                        config.getString("efi.pix-key"),
                        config.getInt("efi.charge-expiration-seconds", 1800)
                ),
                config.getLong("sync.interval-ticks", 200L)
        );
    }

    public List<VipPlanDefinition> loadVipPlans() {
        Path vipPlansPath = resolve("vip-plans.yml");
        try {
            VipPlansFile file = objectMapper.readValue(vipPlansPath.toFile(), VipPlansFile.class);
            return file.plans().entrySet().stream()
                    .map(entry -> new VipPlanDefinition(
                            entry.getKey(),
                            entry.getValue().displayName(),
                            entry.getValue().amount(),
                            entry.getValue().currency(),
                            entry.getValue().luckPermsGroup(),
                            entry.getValue().discordRoleId(),
                            entry.getValue().durationDays(),
                            entry.getValue().active(),
                            entry.getValue().sortOrder()
                    ))
                    .sorted(Comparator.comparingInt(VipPlanDefinition::sortOrder))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load vip-plans.yml", exception);
        }
    }

    private Path resolve(String fileName) {
        Path path = plugin.getDataFolder().toPath().resolve(fileName);
        if (Files.exists(path)) {
            return path;
        }
        try {
            Files.createDirectories(path.getParent());
            try (InputStream inputStream = plugin.getResource(fileName)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Missing bundled resource " + fileName);
                }
                Files.copy(inputStream, path);
            }
            return path;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resolve " + fileName, exception);
        }
    }
}
