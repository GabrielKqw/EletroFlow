package com.eletroflow.plugin.config;

public record WebhookSettings(
        boolean enabled,
        String bindAddress,
        int port,
        String path,
        String publicUrl,
        String token,
        boolean autoRegister,
        boolean skipMtlsChecking
) {
}
