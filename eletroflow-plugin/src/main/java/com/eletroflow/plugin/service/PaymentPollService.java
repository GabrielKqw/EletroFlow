package com.eletroflow.plugin.service;

import com.eletroflow.plugin.EletroFlowPlugin;
import com.eletroflow.plugin.LuckPermsProvisionService;
import com.eletroflow.plugin.efi.EfiPixClient;
import com.eletroflow.plugin.model.PaymentCheckResult;
import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.model.PlanRecord;
import com.eletroflow.plugin.storage.AuditLogRepository;
import com.eletroflow.plugin.storage.PaymentRepository;
import com.eletroflow.plugin.storage.PaymentTransactionRepository;
import com.eletroflow.plugin.storage.PlanRepository;
import com.eletroflow.plugin.storage.VipGrantRepository;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;

public class PaymentPollService {

    private final EletroFlowPlugin plugin;
    private final PaymentRepository paymentRepository;
    private final PlanRepository planRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final VipGrantRepository vipGrantRepository;
    private final AuditLogRepository auditLogRepository;
    private final EfiPixClient efiPixClient;
    private final LuckPermsProvisionService luckPermsProvisionService;
    private final DiscordBotService discordBotService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean active = new AtomicBoolean(false);
    private Thread workerThread;

    public PaymentPollService(
            EletroFlowPlugin plugin,
            PaymentRepository paymentRepository,
            PlanRepository planRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            VipGrantRepository vipGrantRepository,
            AuditLogRepository auditLogRepository,
            EfiPixClient efiPixClient,
            LuckPermsProvisionService luckPermsProvisionService,
            DiscordBotService discordBotService
    ) {
        this.plugin = plugin;
        this.paymentRepository = paymentRepository;
        this.planRepository = planRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.vipGrantRepository = vipGrantRepository;
        this.auditLogRepository = auditLogRepository;
        this.efiPixClient = efiPixClient;
        this.luckPermsProvisionService = luckPermsProvisionService;
        this.discordBotService = discordBotService;
    }

    public void start(long intervalTicks) {
        long intervalMillis = Math.max(1_000L, intervalTicks * 50L);
        if (!active.compareAndSet(false, true)) {
            return;
        }
        workerThread = new Thread(() -> runLoop(intervalMillis), "EletroFlow-PaymentPoll");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void stop() {
        active.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    private void runLoop(long intervalMillis) {
        try {
            Thread.sleep(3_000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return;
        }
        while (active.get() && !Thread.currentThread().isInterrupted()) {
            try {
                poll();
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Falha no ciclo de verificacao Pix", exception);
            }
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void poll() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            for (PaymentRecord payment : paymentRepository.findPendingPayments()) {
                if (payment.rewardedAt() != null) {
                    continue;
                }
                PaymentCheckResult result = efiPixClient.checkPayment(payment.txid());
                if (!result.confirmed()) {
                    continue;
                }
                if (paymentTransactionRepository.existsByProviderEventId(result.endToEndId())) {
                    continue;
                }
                paymentRepository.markConfirmed(payment.id(), result);
                paymentTransactionRepository.save(payment.id(), result);
                auditLogRepository.save("PAYMENT", payment.id(), "PAYMENT_CONFIRMED", "eventId=" + result.endToEndId());
                PlanRecord plan = planRepository.findRequiredPlan(payment.planKey());
                if (vipGrantRepository.existsByPaymentId(payment.id())) {
                    continue;
                }
                luckPermsProvisionService.grant(payment, plan);
                vipGrantRepository.saveGranted(payment, plan);
                paymentRepository.markRewarded(payment.id());
                auditLogRepository.save("VIP_GRANT", payment.id(), "VIP_GRANTED", "group=" + plan.luckPermsGroup());
                discordBotService.notifyApprovedPayment(payment, plan, result);
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Falha ao processar pagamentos Pix", exception);
        } finally {
            running.set(false);
        }
    }
}
