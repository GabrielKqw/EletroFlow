package com.eletroflow.plugin.config;

public record PluginSettings(
        String serverId,
        DatabaseSettings database,
        DiscordSettings discord,
        EfiSettings efi,
        long syncIntervalTicks
) {
}

