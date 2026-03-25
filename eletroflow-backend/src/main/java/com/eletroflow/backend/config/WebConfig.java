package com.eletroflow.backend.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder().build();
    }
}
