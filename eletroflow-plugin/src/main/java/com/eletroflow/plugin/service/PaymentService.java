package com.eletroflow.plugin.service;

import com.eletroflow.plugin.efi.EfiPixClient;
import com.eletroflow.plugin.model.PaymentCreation;
import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.model.PlanRecord;
import com.eletroflow.plugin.storage.PaymentRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EfiPixClient efiPixClient;

    public PaymentService(PaymentRepository paymentRepository, EfiPixClient efiPixClient) {
        this.paymentRepository = paymentRepository;
        this.efiPixClient = efiPixClient;
    }

    public PaymentRecord createOrReusePayment(
            String discordId,
            String minecraftUuid,
            String minecraftUsername,
            String threadId,
            PlanRecord plan
    ) {
        validatePlayerIdentity(minecraftUuid, minecraftUsername);
        Optional<PaymentRecord> reusable = paymentRepository.findReusablePendingPayment(discordId, plan.key());
        if (reusable.isPresent()) {
            return reusable.get();
        }
        EfiPixClient.PixCharge pixCharge = efiPixClient.createCharge(plan.amount(), "VIP " + plan.displayName());
        PaymentCreation creation = new PaymentCreation(
                UUID.randomUUID().toString(),
                discordId,
                minecraftUuid,
                minecraftUsername,
                plan.key(),
                plan.amount(),
                pixCharge.txid(),
                pixCharge.copyPasteCode(),
                pixCharge.qrCodeBase64(),
                threadId,
                pixCharge.expiresAt()
        );
        paymentRepository.savePayment(creation);
        return new PaymentRecord(
                creation.id(),
                creation.discordId(),
                creation.minecraftUuid(),
                creation.minecraftUsername(),
                creation.planKey(),
                creation.amount(),
                creation.txid(),
                creation.copyPasteCode(),
                creation.qrCodeBase64(),
                creation.discordThreadId(),
                com.eletroflow.shared.enums.PaymentStatus.PENDING,
                creation.expiresAt(),
                OffsetDateTime.now(),
                null,
                null
        );
    }

    private void validatePlayerIdentity(String minecraftUuid, String minecraftUsername) {
        try {
            UUID.fromString(minecraftUuid);
        } catch (Exception exception) {
            throw new IllegalArgumentException("UUID do Minecraft invalido");
        }
        if (minecraftUsername == null || minecraftUsername.isBlank() || minecraftUsername.length() > 16) {
            throw new IllegalArgumentException("Nick do Minecraft invalido");
        }
    }
}
