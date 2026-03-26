package com.eletroflow.plugin.efi;

import com.eletroflow.plugin.config.EfiSettings;
import com.eletroflow.plugin.model.PaymentCheckResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
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

public class EfiPixClient {

    private static final MediaType JSON = MediaType.get("application/json");

    private final ObjectMapper objectMapper;
    private final EfiSettings settings;
    private final OkHttpClient httpClient;
    private CachedToken cachedToken;

    public EfiPixClient(ObjectMapper objectMapper, EfiSettings settings) {
        this.objectMapper = objectMapper;
        this.settings = settings;
        this.httpClient = buildHttpClient();
    }

    public PixCharge createCharge(BigDecimal amount, String description) {
        String txid = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        ChargeRequest chargeRequest = new ChargeRequest(
                new ChargeCalendar(settings.chargeExpirationSeconds()),
                new ChargeValue(amount.setScale(2).toPlainString()),
                settings.pixKey(),
                description
        );
        ChargeResponse chargeResponse = executeAuthorizedRequest(
                buildBaseUrl().newBuilder().addPathSegments("v2/cob/" + txid).build(),
                "PUT",
                chargeRequest,
                ChargeResponse.class
        );
        QrCodeResponse qrCodeResponse = executeAuthorizedRequest(
                buildBaseUrl().newBuilder().addPathSegments("v2/loc/" + chargeResponse.loc().id() + "/qrcode").build(),
                "GET",
                null,
                QrCodeResponse.class
        );
        return new PixCharge(
                chargeResponse.txid(),
                qrCodeResponse.qrCode(),
                qrCodeResponse.imagemQrcode(),
                OffsetDateTime.parse(chargeResponse.calendario().criacao()).plusSeconds(Long.parseLong(chargeResponse.calendario().expiracao()))
        );
    }

    public PaymentCheckResult checkPayment(String txid) {
        ChargeStatusResponse response = executeAuthorizedRequest(
                buildBaseUrl().newBuilder().addPathSegments("v2/cob/" + txid).build(),
                "GET",
                null,
                ChargeStatusResponse.class
        );
        if (!"CONCLUIDA".equalsIgnoreCase(response.status())) {
            return new PaymentCheckResult(false, null, null);
        }
        PixReceipt pix = response.pix() == null || response.pix().isEmpty() ? null : response.pix().getFirst();
        return new PaymentCheckResult(
                true,
                pix == null ? OffsetDateTime.now() : pix.horario(),
                pix == null ? txid : pix.endToEndId()
        );
    }

    private synchronized String accessToken() {
        if (cachedToken != null && cachedToken.expiresAt().isAfter(OffsetDateTime.now().plusSeconds(30))) {
            return cachedToken.value();
        }
        String credentials = Base64.getEncoder().encodeToString(
                (settings.clientId() + ":" + settings.clientSecret()).getBytes(StandardCharsets.UTF_8)
        );
        Request request = new Request.Builder()
                .url(buildBaseUrl().newBuilder().addPathSegments("oauth/token").build())
                .post(new FormBody.Builder().add("grant_type", "client_credentials").build())
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("Failed to authenticate with EFI");
            }
            OAuthToken token = objectMapper.readValue(response.body().string(), OAuthToken.class);
            cachedToken = new CachedToken(token.accessToken(), OffsetDateTime.now().plusSeconds(token.expiresIn()));
            return cachedToken.value();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to request EFI token", exception);
        }
    }

    private <T> T executeAuthorizedRequest(HttpUrl url, String method, Object payload, Class<T> responseType) {
        try {
            RequestBody body = payload == null ? null : RequestBody.create(objectMapper.writeValueAsString(payload), JSON);
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + accessToken());
            if ("PUT".equals(method)) {
                builder.put(body);
            } else {
                builder.get();
            }
            try (Response response = httpClient.newCall(builder.build()).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IllegalStateException("EFI request failed with status " + response.code());
                }
                return objectMapper.readValue(response.body().string(), responseType);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call EFI", exception);
        }
    }

    private OkHttpClient buildHttpClient() {
        try {
            char[] password = settings.certificatePassword() == null ? new char[0] : settings.certificatePassword().toCharArray();
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream inputStream = new FileInputStream(settings.certificatePath())) {
                keyStore.load(inputStream, password);
            }
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            X509TrustManager trustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to configure EFI mTLS", exception);
        }
    }

    private HttpUrl buildBaseUrl() {
        return HttpUrl.get(Objects.requireNonNull(settings.baseUrl()));
    }

    private record CachedToken(String value, OffsetDateTime expiresAt) {
    }

    private record ChargeRequest(
            @JsonProperty("calendario") ChargeCalendar calendario,
            @JsonProperty("valor") ChargeValue valor,
            @JsonProperty("chave") String chave,
            @JsonProperty("solicitacaoPagador") String solicitacaoPagador
    ) {
    }

    private record ChargeCalendar(@JsonProperty("expiracao") int expiracao) {
    }

    private record ChargeValue(@JsonProperty("original") String original) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OAuthToken(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") int expiresIn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChargeResponse(String txid, ChargeCalendarResponse calendario, ChargeLocation loc) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChargeCalendarResponse(String criacao, String expiracao) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChargeLocation(Integer id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QrCodeResponse(@JsonProperty("qrcode") String qrCode, @JsonProperty("imagemQrcode") String imagemQrcode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChargeStatusResponse(String status, java.util.List<PixReceipt> pix) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PixReceipt(String endToEndId, OffsetDateTime horario) {
    }

    public record PixCharge(
            String txid,
            String copyPasteCode,
            String qrCodeBase64,
            OffsetDateTime expiresAt
    ) {
    }
}

