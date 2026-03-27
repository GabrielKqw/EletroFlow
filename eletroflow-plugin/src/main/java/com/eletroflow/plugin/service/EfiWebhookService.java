package com.eletroflow.plugin.service;

import com.eletroflow.plugin.EletroFlowPlugin;
import com.eletroflow.plugin.config.WebhookSettings;
import com.eletroflow.plugin.efi.EfiPixClient;
import com.eletroflow.plugin.model.PaymentCheckResult;
import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.storage.PaymentRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class EfiWebhookService {

    private final EletroFlowPlugin plugin;
    private final WebhookSettings settings;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final PaymentConfirmationService paymentConfirmationService;
    private final EfiPixClient efiPixClient;
    private HttpServer httpServer;
    private ExecutorService executorService;

    public EfiWebhookService(
            EletroFlowPlugin plugin,
            WebhookSettings settings,
            ObjectMapper objectMapper,
            PaymentRepository paymentRepository,
            PaymentConfirmationService paymentConfirmationService,
            EfiPixClient efiPixClient
    ) {
        this.plugin = plugin;
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.paymentRepository = paymentRepository;
        this.paymentConfirmationService = paymentConfirmationService;
        this.efiPixClient = efiPixClient;
    }

    public void start() {
        if (!settings.enabled()) {
            plugin.getLogger().info("EFI webhook disabled, keeping Pix reconciliation on poller");
            return;
        }
        try {
            httpServer = HttpServer.create(new InetSocketAddress(settings.bindAddress(), settings.port()), 0);
            HttpHandler handler = this::handle;
            httpServer.createContext(normalizePath(settings.path()), handler);
            httpServer.createContext(normalizePath(settings.path()) + "/pix", handler);
            executorService = Executors.newFixedThreadPool(2);
            httpServer.setExecutor(executorService);
            httpServer.start();
            plugin.getLogger().info("EFI webhook listening on " + settings.bindAddress() + ":" + settings.port() + normalizePath(settings.path()));
            if (settings.autoRegister()) {
                efiPixClient.registerWebhook(settings.publicUrl(), settings.skipMtlsChecking());
                plugin.getLogger().info("EFI webhook registered for Pix key");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to start EFI webhook listener", exception);
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "method-not-allowed");
                return;
            }
            if (!settings.token().equals(readToken(exchange))) {
                respond(exchange, 401, "unauthorized");
                return;
            }
            byte[] body = exchange.getRequestBody().readAllBytes();
            if (body.length == 0) {
                respond(exchange, 200, "ok");
                return;
            }
            WebhookPayload payload = objectMapper.readValue(body, WebhookPayload.class);
            if (payload.pix() == null || payload.pix().isEmpty()) {
                respond(exchange, 200, "ok");
                return;
            }
            int confirmed = 0;
            for (WebhookPix webhookPix : payload.pix()) {
                if (webhookPix.txid() == null || webhookPix.txid().isBlank()) {
                    continue;
                }
                Optional<PaymentRecord> payment = paymentRepository.findByTxid(webhookPix.txid());
                if (payment.isEmpty()) {
                    plugin.getLogger().warning("EFI webhook received unknown txid " + webhookPix.txid());
                    continue;
                }
                PaymentCheckResult result = new PaymentCheckResult(
                        true,
                        parseTime(webhookPix.horario()),
                        webhookPix.endToEndId()
                );
                if (paymentConfirmationService.confirm(payment.get(), result, "webhook")) {
                    confirmed++;
                }
            }
            plugin.getLogger().info("EFI webhook processed " + payload.pix().size() + " Pix events and confirmed " + confirmed);
            respond(exchange, 200, "ok");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to process EFI webhook", exception);
            respond(exchange, 500, "error");
        }
    }

    private OffsetDateTime parseTime(String value) {
        return value == null || value.isBlank() ? OffsetDateTime.now() : OffsetDateTime.parse(value);
    }

    private String readToken(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return "";
        }
        for (String segment : query.split("&")) {
            int separator = segment.indexOf('=');
            if (separator < 0) {
                continue;
            }
            if ("token".equals(segment.substring(0, separator))) {
                return java.net.URLDecoder.decode(segment.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/eletroflow/webhook";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WebhookPayload(@JsonProperty("pix") List<WebhookPix> pix) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WebhookPix(
            @JsonProperty("txid") String txid,
            @JsonProperty("endToEndId") String endToEndId,
            @JsonProperty("horario") String horario
    ) {
    }
}
