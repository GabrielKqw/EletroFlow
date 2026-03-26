package com.eletroflow.plugin.service;

import com.eletroflow.plugin.efi.EfiPixClient;
import com.eletroflow.plugin.model.PaymentCreation;
import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.model.PlanRecord;
import com.eletroflow.plugin.model.UserRecord;
import com.eletroflow.plugin.storage.AuditLogRepository;
import com.eletroflow.plugin.storage.PaymentRepository;
import com.eletroflow.plugin.storage.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final EfiPixClient efiPixClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            EfiPixClient efiPixClient
    ) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
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
        UserRecord user = userRepository.upsert(discordId, minecraftUuid, minecraftUsername);
        Optional<PaymentRecord> reusable = paymentRepository.findReusablePendingPayment(discordId, plan.key());
        if (reusable.isPresent()) {
            auditLogRepository.save("PAYMENT", reusable.get().id(), "PIX_REUSED", "txid=" + reusable.get().txid());
            return reusable.get();
        }
        EfiPixClient.PixCharge pixCharge = efiPixClient.createCharge(plan.amount(), "VIP " + plan.displayName());
        PaymentCreation creation = new PaymentCreation(
                UUID.randomUUID().toString(),
                user.id(),
                plan.key(),
                plan.amount(),
                pixCharge.txid(),
                pixCharge.copyPasteCode(),
                pixCharge.qrCodeBase64(),
                threadId,
                pixCharge.expiresAt()
        );
        paymentRepository.savePayment(creation);
        PaymentRecord paymentRecord = new PaymentRecord(
                creation.id(),
                creation.userId(),
                user.discordId(),
                user.minecraftUuid(),
                user.minecraftUsername(),
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
        auditLogRepository.save("PAYMENT", paymentRecord.id(), "PIX_CREATED", "txid=" + paymentRecord.txid());
        return paymentRecord;
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
