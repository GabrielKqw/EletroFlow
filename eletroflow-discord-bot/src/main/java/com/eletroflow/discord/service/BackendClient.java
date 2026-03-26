package com.eletroflow.discord.service;

import com.eletroflow.discord.config.BotConfiguration;
import com.eletroflow.shared.dto.CreatePaymentRequest;
import com.eletroflow.shared.dto.PaymentResponse;
import com.eletroflow.shared.dto.PaymentStatusResponse;
import com.eletroflow.shared.dto.VipPlanResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackendClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient = new OkHttpClient.Builder().build();
    private final BotConfiguration configuration;
    private final ObjectMapper objectMapper;

    public BackendClient(BotConfiguration configuration, ObjectMapper objectMapper) {
        this.configuration = configuration;
        this.objectMapper = objectMapper;
    }

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        try {
            String body = objectMapper.writeValueAsString(request);
            Request httpRequest = new Request.Builder()
                    .url(configuration.backendBaseUrl() + "/api/payments")
                    .post(RequestBody.create(body, JSON))
                    .header("Content-Type", "application/json")
                    .build();
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IllegalStateException("Payment creation failed with status " + response.code());
                }
                return objectMapper.readValue(response.body().string(), PaymentResponse.class);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call backend", exception);
        }
    }

    public PaymentStatusResponse getPaymentStatus(String paymentId) {
        Request request = new Request.Builder()
                .url(configuration.backendBaseUrl() + "/api/payments/" + paymentId)
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("Payment lookup failed with status " + response.code());
            }
            return objectMapper.readValue(response.body().string(), PaymentStatusResponse.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call backend", exception);
        }
    }

    public List<VipPlanResponse> listVipPlans() {
        Request request = new Request.Builder()
                .url(configuration.backendBaseUrl() + "/api/vip-plans")
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("VIP plan lookup failed with status " + response.code());
            }
            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call backend", exception);
        }
    }
}
