package com.eletroflow.discord.config;

public record BotConfiguration(
        String token,
        String guildId,
        String ticketPanelChannelId,
        String supportRoleId,
        String backendBaseUrl,
        long paymentPollIntervalSeconds
) {
}
