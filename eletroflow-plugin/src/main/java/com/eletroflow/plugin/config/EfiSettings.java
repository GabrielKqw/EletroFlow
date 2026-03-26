package com.eletroflow.plugin.config;

public record EfiSettings(
        String baseUrl,
        String clientId,
        String clientSecret,
        String certificatePath,
        String certificatePassword,
        String pixKey,
        int chargeExpirationSeconds
) {
}

