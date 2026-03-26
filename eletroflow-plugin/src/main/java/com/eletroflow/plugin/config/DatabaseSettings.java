package com.eletroflow.plugin.config;

public record DatabaseSettings(
        String jdbcUrl,
        String username,
        String password
) {
}

