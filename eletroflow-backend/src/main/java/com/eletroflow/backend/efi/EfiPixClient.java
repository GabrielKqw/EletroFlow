package com.eletroflow.backend.efi;

import com.eletroflow.backend.config.EfiPixProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

@Component
public class EfiPixClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final ObjectMapper objectMapper;
    private final EfiPixProperties properties;
    private volatile CachedToken cachedToken;

    public EfiPixClient(OkHttpClient httpClient, ObjectMapper objectMapper, EfiPixProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = buildHttpClient(httpClient);
    }

    private final OkHttpClient httpClient;

    public PixChargeResponse createCharge(BigDecimal amount, String payerReference) {
        validateConfiguration();
        String txid = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        ChargeRequest requestPayload = new ChargeRequest(
                new Calendar(properties.chargeExpirationSeconds()),
                new ChargeValue(amount.setScale(2).toPlainString()),
                properties.pixKey(),
                "VIP " + payerReference
        );
        ChargeResponse charge = executeAuthorizedRequest(
                HttpUrl.get(Objects.requireNonNull(properties.baseUrl())).newBuilder()
                        .addPathSegments("v2/cob/" + txid)
                        .build(),
                "PUT",
                requestPayload,
                ChargeResponse.class
        );
        if (charge.loc() == null || charge.loc().id() == null) {
            throw new IllegalStateException("EFI charge response is missing location id");
        }
        QrCodeResponse qrCode = executeAuthorizedRequest(
                HttpUrl.get(Objects.requireNonNull(properties.baseUrl())).newBuilder()
                        .addPathSegments("v2/loc/" + charge.loc().id() + "/qrcode")
                        .build(),
                "GET",
                null,
                QrCodeResponse.class
        );
        if (qrCode.qrCode() == null || qrCode.qrCode().isBlank()) {
            throw new IllegalStateException("EFI QR code response is missing pix copia e cola");
        }
        return new PixChargeResponse(
                charge.txid(),
                qrCode.qrCode(),
                qrCode.imagemQrcode(),
                OffsetDateTime.parse(charge.calendario().criacao()).plusSeconds(Long.parseLong(charge.calendario().expiracao()))
        );
    }

    public boolean validateWebhookSecret(String providedSecret) {
        if (properties.webhookSecret() == null || properties.webhookSecret().isBlank()) {
            throw new IllegalStateException("Webhook secret is not configured");
        }
        if (providedSecret == null || providedSecret.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                properties.webhookSecret().getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    private OkHttpClient buildHttpClient(OkHttpClient baseClient) {
        if (properties.certificatePath() == null || properties.certificatePath().isBlank()) {
            return baseClient.newBuilder().build();
        }
        try {
            char[] password = properties.certificatePassword() == null ? new char[0] : properties.certificatePassword().toCharArray();
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream inputStream = new FileInputStream(properties.certificatePath())) {
                keyStore.load(inputStream, password);
            }
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            X509TrustManager trustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            return baseClient.newBuilder()
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize EFI mTLS client", exception);
        }
    }

    private void validateConfiguration() {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new IllegalStateException("EFI base URL is not configured");
        }
        if (properties.clientId() == null || properties.clientId().isBlank()) {
            throw new IllegalStateException("EFI client id is not configured");
        }
        if (properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new IllegalStateException("EFI client secret is not configured");
        }
        if (properties.pixKey() == null || properties.pixKey().isBlank()) {
            throw new IllegalStateException("EFI Pix key is not configured");
        }
    }

    private synchronized String getAccessToken() {
        if (cachedToken != null && cachedToken.expiresAt().isAfter(OffsetDateTime.now().plusSeconds(30))) {
            return cachedToken.value();
        }
        HttpUrl url = HttpUrl.get(Objects.requireNonNull(properties.baseUrl())).newBuilder()
                .addPathSegments("oauth/token")
                .build();
        String basicCredentials = Base64.getEncoder().encodeToString(
                (properties.clientId() + ":" + properties.clientSecret()).getBytes(StandardCharsets.UTF_8)
        );
        Request request = new Request.Builder()
                .url(url)
                .post(new FormBody.Builder().add("grant_type", "client_credentials").build())
                .header("Authorization", "Basic " + basicCredentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("EFI token request failed with status " + response.code());
            }
            OAuthTokenResponse tokenResponse = objectMapper.readValue(response.body().string(), OAuthTokenResponse.class);
            cachedToken = new CachedToken(
                    tokenResponse.accessToken(),
                    OffsetDateTime.now().plusSeconds(tokenResponse.expiresIn())
            );
            return cachedToken.value();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to obtain EFI access token", exception);
        }
    }

    private <T> T executeAuthorizedRequest(HttpUrl url, String method, Object payload, Class<T> responseType) {
        try {
            RequestBody body = null;
            if (payload != null) {
                body = RequestBody.create(objectMapper.writeValueAsString(payload), JSON);
            }
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + getAccessToken());
            if ("PUT".equals(method)) {
                builder.put(body);
            } else if ("POST".equals(method)) {
                builder.post(body);
            } else {
                builder.get();
            }
            try (Response response = httpClient.newCall(builder.build()).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IllegalStateException("EFI request failed with status " + response.code() + " for " + url.encodedPath());
                }
                return objectMapper.readValue(response.body().string(), responseType);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call EFI API", exception);
        }
    }

    private record CachedToken(String value, OffsetDateTime expiresAt) {
    }

    private record ChargeRequest(
            @JsonProperty("calendario") Calendar calendario,
            @JsonProperty("valor") ChargeValue valor,
            @JsonProperty("chave") String pixKey,
            @JsonProperty("solicitacaoPagador") String payerRequest
    ) {
    }

    private record Calendar(
            @JsonProperty("expiracao") int expiracao
    ) {
    }

    private record ChargeValue(
            @JsonProperty("original") String originalValue
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OAuthTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") int expiresIn
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChargeResponse(
            String txid,
            ChargeCalendarResponse calendario,
            ChargeLocation loc
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChargeCalendarResponse(
            String criacao,
            String expiracao
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChargeLocation(
            Integer id
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QrCodeResponse(
            @JsonProperty("qrcode") String qrCode,
            @JsonProperty("imagemQrcode") String imagemQrcode
    ) {
    }

    public record PixChargeResponse(
            String txid,
            String copyPasteCode,
            String qrCodeBase64,
            OffsetDateTime expiresAt
    ) {
    }
}
