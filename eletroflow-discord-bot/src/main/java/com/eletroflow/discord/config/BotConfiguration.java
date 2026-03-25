package com.eletroflow.discord.config;

public record BotConfiguration(
        String token,
        String guildId,
        String ticketCategoryId,
        String supportRoleId,
        String backendBaseUrl,
        long paymentPollIntervalSeconds
) {
}
