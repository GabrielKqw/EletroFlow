package com.eletroflow.plugin.config;

public record PluginSettings(
        String serverId,
        MinecraftSettings minecraft,
        DatabaseSettings database,
        DiscordSettings discord,
        EfiSettings efi,
        long syncIntervalTicks
) {
}
