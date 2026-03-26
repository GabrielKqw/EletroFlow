package com.eletroflow.plugin.model;

import java.time.OffsetDateTime;

public record PaymentCheckResult(
        boolean confirmed,
        OffsetDateTime confirmedAt,
        String endToEndId
) {
}
