package com.eletroflow.plugin.service;

import com.eletroflow.shared.dto.PendingRewardResponse;
import com.eletroflow.shared.dto.RewardAckRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bukkit.configuration.file.FileConfiguration;

public class BackendSyncClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final FileConfiguration configuration;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    public BackendSyncClient(FileConfiguration configuration, ObjectMapper objectMapper) {
        this.configuration = configuration;
        this.objectMapper = objectMapper;
    }

    public List<PendingRewardResponse> claimPendingRewards() {
        Request request = new Request.Builder()
                .url(configuration.getString("backend.base-url") + "/api/rewards/pending")
                .get()
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("Reward claim failed with status " + response.code());
            }
            return objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch rewards", exception);
        }
    }

    public void markCompleted(String rewardId, String externalReference) {
        sendAck(rewardId, "complete", new RewardAckRequest(
                configuration.getString("server.id"),
                externalReference,
                null
        ));
    }

    public void markFailed(String rewardId, String externalReference, String failureReason) {
        sendAck(rewardId, "fail", new RewardAckRequest(
                configuration.getString("server.id"),
                externalReference,
                failureReason
        ));
    }

    private void sendAck(String rewardId, String action, RewardAckRequest requestPayload) {
        try {
            String body = objectMapper.writeValueAsString(requestPayload);
            Request request = new Request.Builder()
                    .url(configuration.getString("backend.base-url") + "/api/rewards/" + rewardId + "/" + action)
                    .post(RequestBody.create(body, JSON))
                    .header("Content-Type", "application/json")
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("Reward ack failed with status " + response.code());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to acknowledge reward", exception);
        }
    }
}
