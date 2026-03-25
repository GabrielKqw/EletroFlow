package com.eletroflow.backend.web;

import com.eletroflow.backend.efi.EfiWebhookPayload;
import com.eletroflow.backend.efi.EfiPixClient;
import com.eletroflow.backend.service.PaymentConfirmationService;
import com.eletroflow.shared.dto.PaymentWebhookRequest;
import java.util.Optional;
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
            @RequestBody EfiWebhookPayload payload
    ) {
        if (!efiPixClient.validateWebhookSecret(providedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook secret");
        }
        if (payload.pix() == null || payload.pix().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook payload does not contain pix events");
        }
        payload.pix().forEach(pixItem -> paymentConfirmationService.confirmPayment(new PaymentWebhookRequest(
                pixItem.txid(),
                pixItem.endToEndId(),
                pixItem.valor(),
                extractPayerDocument(pixItem),
                pixItem.horario()
        )));
    }

    private String extractPayerDocument(EfiWebhookPayload.PixItem pixItem) {
        return Optional.ofNullable(pixItem.gnExtras())
                .map(EfiWebhookPayload.GnExtras::pagador)
                .map(pagador -> pagador.cpf() != null ? pagador.cpf() : pagador.cnpj())
                .orElse(null);
    }
}
