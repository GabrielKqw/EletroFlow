package com.eletroflow.backend.web;

import com.eletroflow.backend.service.PaymentService;
import com.eletroflow.shared.dto.CreatePaymentRequest;
import com.eletroflow.shared.dto.PaymentResponse;
import com.eletroflow.shared.dto.PaymentStatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping("/{paymentId}")
    public PaymentStatusResponse getPaymentStatus(@PathVariable String paymentId) {
        return paymentService.getPaymentStatus(paymentId);
    }
}
