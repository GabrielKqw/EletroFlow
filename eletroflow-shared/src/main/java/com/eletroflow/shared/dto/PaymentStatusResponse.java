package com.eletroflow.shared.dto;

import com.eletroflow.shared.enums.PaymentStatus;
import com.eletroflow.shared.enums.ProvisionStatus;

public record PaymentStatusResponse(
        String paymentId,
        PaymentStatus paymentStatus,
        ProvisionStatus provisionStatus,
        String txid
) {
}
