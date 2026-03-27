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
import java.util.logging.Logger;

public class PaymentService {

    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final MinecraftIdentityService minecraftIdentityService;
    private final EfiPixClient efiPixClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            MinecraftIdentityService minecraftIdentityService,
            EfiPixClient efiPixClient
    ) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.minecraftIdentityService = minecraftIdentityService;
        this.efiPixClient = efiPixClient;
    }

    public PaymentRecord createOrReusePayment(
            String discordId,
            String minecraftUsername,
            String payerCpf,
            String threadId,
            PlanRecord plan
    ) {
        MinecraftIdentityService.ResolvedMinecraftIdentity identity = minecraftIdentityService.resolve(minecraftUsername);
        String normalizedCpf = normalizeCpf(payerCpf);
        UserRecord user = userRepository.upsert(discordId, identity.uuid(), identity.username());
        Optional<PaymentRecord> reusable = paymentRepository.findReusablePendingPayment(
                discordId,
                plan.key(),
                identity.username(),
                normalizedCpf,
                threadId
        );
        if (reusable.isPresent()) {
            PaymentRecord existingPayment = reusable.get();
            EfiPixClient.ChargeStatusSnapshot snapshot = efiPixClient.getChargeStatusSnapshot(existingPayment.txid());
            if (snapshot.concluded()) {
                LOGGER.info("Pix already concluded for txid " + existingPayment.txid() + ", waiting for confirmation delivery");
                throw new IllegalStateException("Seu pagamento anterior ja foi identificado e esta em processamento. Aguarde alguns instantes.");
            }
            if (snapshot.active() && existingPayment.expiresAt().isAfter(OffsetDateTime.now())) {
                LOGGER.info("Reusing active Pix charge txid " + existingPayment.txid() + " for Discord " + discordId);
                auditLogRepository.save("PAYMENT", existingPayment.id(), "PIX_REUSED", "txid=" + existingPayment.txid());
                return existingPayment;
            }
        }
        EfiPixClient.PixCharge pixCharge = efiPixClient.createCharge(
                plan.amount(),
                "VIP " + plan.displayName(),
                identity.username(),
                normalizedCpf
        );
        LOGGER.info("Created new Pix charge txid " + pixCharge.txid() + " for Discord " + discordId + " and plan " + plan.key());
        PaymentCreation creation = new PaymentCreation(
                UUID.randomUUID().toString(),
                user.id(),
                plan.key(),
                plan.amount(),
                pixCharge.txid(),
                identity.username(),
                normalizedCpf,
                pixCharge.copyPasteCode(),
                pixCharge.qrCodeBase64(),
                pixCharge.qrCodeUrl(),
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
                creation.payerName(),
                creation.payerCpf(),
                creation.copyPasteCode(),
                creation.qrCodeBase64(),
                creation.qrCodeUrl(),
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

    private String normalizeCpf(String payerCpf) {
        if (payerCpf == null) {
            throw new IllegalArgumentException("CPF do pagador invalido");
        }
        String normalized = payerCpf.replaceAll("\\D", "");
        if (normalized.length() != 11) {
            throw new IllegalArgumentException("CPF do pagador invalido");
        }
        return normalized;
    }
}
