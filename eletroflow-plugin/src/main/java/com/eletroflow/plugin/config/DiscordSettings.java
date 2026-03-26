package com.eletroflow.plugin.config;

public record DiscordSettings(
        String token,
        String guildId,
        String panelChannelId,
        String supportRoleId,
        long paymentPollIntervalSeconds
) {
}

