package com.eletroflow.plugin.service;

import com.eletroflow.plugin.LuckPermsProvisionService;
import com.eletroflow.plugin.model.PaymentCheckResult;
import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.model.PlanRecord;
import com.eletroflow.plugin.storage.AuditLogRepository;
import com.eletroflow.plugin.storage.PaymentRepository;
import com.eletroflow.plugin.storage.PaymentTransactionRepository;
import com.eletroflow.plugin.storage.PlanRepository;
import com.eletroflow.plugin.storage.VipGrantRepository;
import java.time.OffsetDateTime;
import java.util.logging.Logger;

public class PaymentConfirmationService {

    private static final Logger LOGGER = Logger.getLogger(PaymentConfirmationService.class.getName());

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PlanRepository planRepository;
    private final VipGrantRepository vipGrantRepository;
    private final AuditLogRepository auditLogRepository;
    private final LuckPermsProvisionService luckPermsProvisionService;
    private final DiscordBotService discordBotService;

    public PaymentConfirmationService(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PlanRepository planRepository,
            VipGrantRepository vipGrantRepository,
            AuditLogRepository auditLogRepository,
            LuckPermsProvisionService luckPermsProvisionService,
            DiscordBotService discordBotService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.planRepository = planRepository;
        this.vipGrantRepository = vipGrantRepository;
        this.auditLogRepository = auditLogRepository;
        this.luckPermsProvisionService = luckPermsProvisionService;
        this.discordBotService = discordBotService;
    }

    public boolean confirm(PaymentRecord payment, PaymentCheckResult result, String source) {
        if (!result.confirmed()) {
            return false;
        }
        if (result.endToEndId() != null && paymentTransactionRepository.existsByProviderEventId(result.endToEndId())) {
            LOGGER.info("Ignoring duplicated Pix confirmation for txid " + payment.txid() + " via " + source);
            return false;
        }
        paymentRepository.markConfirmed(payment.id(), result);
        paymentTransactionRepository.save(payment.id(), result);
        auditLogRepository.save("PAYMENT", payment.id(), "PAYMENT_CONFIRMED", "source=" + source + ", eventId=" + result.endToEndId());
        PlanRecord plan = planRepository.findRequiredPlan(payment.planKey());
        if (vipGrantRepository.existsByPaymentId(payment.id())) {
            LOGGER.info("VIP already granted for txid " + payment.txid());
            return false;
        }
        OffsetDateTime activeUntil = vipGrantRepository.findLatestActiveExpiry(payment.userId(), plan.luckPermsGroup());
        OffsetDateTime expiresAt;
        try {
            expiresAt = luckPermsProvisionService.grant(payment, plan, activeUntil);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to deliver LuckPerms group " + plan.luckPermsGroup(), exception);
        }
        vipGrantRepository.saveGranted(payment, plan, expiresAt);
        paymentRepository.markRewarded(payment.id());
        auditLogRepository.save("VIP_GRANT", payment.id(), "VIP_GRANTED", "group=" + plan.luckPermsGroup() + ", expiresAt=" + expiresAt);
        discordBotService.notifyApprovedPayment(payment, plan, result);
        LOGGER.info("Confirmed Pix txid " + payment.txid() + " and delivered group " + plan.luckPermsGroup() + " until " + expiresAt);
        return true;
    }
}
