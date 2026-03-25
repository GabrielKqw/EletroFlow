package com.eletroflow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eletroflow.efi")
public record EfiPixProperties(
        String baseUrl,
        String clientId,
        String clientSecret,
        String certificatePath,
        String certificatePassword,
        String pixKey,
        int chargeExpirationSeconds,
        String webhookSecret
) {
}
