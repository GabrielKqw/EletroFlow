package com.eletroflow.plugin.service;

import com.eletroflow.plugin.EletroFlowPlugin;
import com.eletroflow.plugin.efi.EfiPixClient;
import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.storage.PaymentRepository;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;

public class PaymentPollService {

    private final EletroFlowPlugin plugin;
    private final PaymentRepository paymentRepository;
    private final EfiPixClient efiPixClient;
    private final PaymentConfirmationService paymentConfirmationService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean active = new AtomicBoolean(false);
    private Thread workerThread;

    public PaymentPollService(
            EletroFlowPlugin plugin,
            PaymentRepository paymentRepository,
            EfiPixClient efiPixClient,
            PaymentConfirmationService paymentConfirmationService
    ) {
        this.plugin = plugin;
        this.paymentRepository = paymentRepository;
        this.efiPixClient = efiPixClient;
        this.paymentConfirmationService = paymentConfirmationService;
    }

    public void start(long intervalTicks) {
        long intervalMillis = Math.max(1_000L, intervalTicks * 50L);
        if (!active.compareAndSet(false, true)) {
            return;
        }
        plugin.getLogger().info("Starting Pix poller with interval " + intervalMillis + "ms");
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
                var result = efiPixClient.checkPayment(payment.txid());
                if (!result.confirmed()) {
                    continue;
                }
                paymentConfirmationService.confirm(payment, result, "poller");
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Falha ao processar pagamentos Pix", exception);
        } finally {
            running.set(false);
        }
    }
}
