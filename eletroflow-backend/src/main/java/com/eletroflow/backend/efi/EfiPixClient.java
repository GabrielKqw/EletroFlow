package com.eletroflow.backend.efi;

import com.eletroflow.backend.config.EfiPixProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

@Component
public class EfiPixClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final EfiPixProperties properties;

    public EfiPixClient(OkHttpClient httpClient, ObjectMapper objectMapper, EfiPixProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public PixChargeResponse createCharge(BigDecimal amount, String payerReference) {
        String txid = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        PixChargeRequest requestPayload = new PixChargeRequest(
                new Calendario(properties.chargeExpirationSeconds()),
                new Valor(amount.setScale(2).toPlainString()),
                properties.pixKey(),
                "VIP " + payerReference
        );
        try {
            String body = objectMapper.writeValueAsString(requestPayload);
            Request request = new Request.Builder()
                    .url(properties.baseUrl() + "/v2/cob/" + txid)
                    .put(RequestBody.create(body, JSON))
                    .header("Content-Type", "application/json")
                    .header("x-skip-mtls", "true")
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("EFI charge creation failed with status " + response.code());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create Pix charge", exception);
        }
        return new PixChargeResponse(
                txid,
                "00020101021226810014br.gov.bcb.pix2563pix.efi.com.br/qr/v2/" + txid,
                "",
                OffsetDateTime.now().plusSeconds(properties.chargeExpirationSeconds())
        );
    }

    public boolean validateWebhookSecret(String providedSecret) {
        return properties.webhookSecret() != null && properties.webhookSecret().equals(providedSecret);
    }

    private record PixChargeRequest(
            Calendario calendario,
            Valor valor,
            @JsonProperty("chave") String pixKey,
            @JsonProperty("solicitacaoPagador") String payerRequest
    ) {
    }

    private record Calendario(int expiracao) {
    }

    private record Valor(@JsonProperty("original") String originalValue) {
    }

    public record PixChargeResponse(
            String txid,
            String copyPasteCode,
            String qrCodeBase64,
            OffsetDateTime expiresAt
    ) {
    }
}
