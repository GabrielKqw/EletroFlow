package com.eletroflow.backend.web;

import com.eletroflow.backend.efi.EfiPixClient;
import com.eletroflow.backend.service.PaymentConfirmationService;
import com.eletroflow.shared.dto.PaymentWebhookRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/webhooks/efi")
public class WebhookController {

    private final PaymentConfirmationService paymentConfirmationService;
    private final EfiPixClient efiPixClient;

    public WebhookController(PaymentConfirmationService paymentConfirmationService, EfiPixClient efiPixClient) {
        this.paymentConfirmationService = paymentConfirmationService;
        this.efiPixClient = efiPixClient;
    }

    @PostMapping("/pix")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receivePixWebhook(
            @RequestHeader(name = "X-Webhook-Secret", required = false) String providedSecret,
            @Valid @RequestBody PaymentWebhookRequest request
    ) {
        if (!efiPixClient.validateWebhookSecret(providedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook secret");
        }
        paymentConfirmationService.confirmPayment(request);
    }
}
