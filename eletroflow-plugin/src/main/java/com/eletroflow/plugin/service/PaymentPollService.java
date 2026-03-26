package com.eletroflow.plugin.service;

import com.eletroflow.plugin.EletroFlowPlugin;
import com.eletroflow.plugin.LuckPermsProvisionService;
import com.eletroflow.plugin.efi.EfiPixClient;
import com.eletroflow.plugin.model.PaymentCheckResult;
import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.model.PlanRecord;
import com.eletroflow.plugin.storage.PaymentRepository;
import com.eletroflow.plugin.storage.PlanRepository;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class PaymentPollService {

    private final EletroFlowPlugin plugin;
    private final PaymentRepository paymentRepository;
    private final PlanRepository planRepository;
    private final EfiPixClient efiPixClient;
    private final LuckPermsProvisionService luckPermsProvisionService;
    private final DiscordBotService discordBotService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private BukkitTask task;

    public PaymentPollService(
            EletroFlowPlugin plugin,
            PaymentRepository paymentRepository,
            PlanRepository planRepository,
            EfiPixClient efiPixClient,
            LuckPermsProvisionService luckPermsProvisionService,
            DiscordBotService discordBotService
    ) {
        this.plugin = plugin;
        this.paymentRepository = paymentRepository;
        this.planRepository = planRepository;
        this.efiPixClient = efiPixClient;
        this.luckPermsProvisionService = luckPermsProvisionService;
        this.discordBotService = discordBotService;
    }

    public void start(long intervalTicks) {
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::poll, 40L, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
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
                paymentRepository.markConfirmed(payment.id(), result);
                PlanRecord plan = planRepository.findRequiredPlan(payment.planKey());
                luckPermsProvisionService.grant(payment, plan);
                paymentRepository.markRewarded(payment.id());
                discordBotService.notifyApprovedPayment(payment, plan);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Falha ao processar pagamentos Pix: " + exception.getMessage());
        } finally {
            running.set(false);
        }
    }
}
