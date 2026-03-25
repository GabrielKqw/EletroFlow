package com.eletroflow.plugin.service;

import com.eletroflow.plugin.EletroFlowPlugin;
import com.eletroflow.plugin.LuckPermsProvisionService;
import com.eletroflow.shared.dto.PendingRewardResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class RewardSyncService {

    private final EletroFlowPlugin plugin;
    private final BackendSyncClient backendSyncClient;
    private final LuckPermsProvisionService luckPermsProvisionService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private BukkitTask task;

    public RewardSyncService(
            EletroFlowPlugin plugin,
            BackendSyncClient backendSyncClient,
            LuckPermsProvisionService luckPermsProvisionService
    ) {
        this.plugin = plugin;
        this.backendSyncClient = backendSyncClient;
        this.luckPermsProvisionService = luckPermsProvisionService;
    }

    public void start() {
        long intervalTicks = plugin.getConfig().getLong("sync.interval-ticks", 200L);
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sync, 40L, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void sync() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            List<PendingRewardResponse> rewards = backendSyncClient.claimPendingRewards();
            for (PendingRewardResponse reward : rewards) {
                processReward(reward);
            }
        } finally {
            running.set(false);
        }
    }

    private void processReward(PendingRewardResponse reward) {
        try {
            String externalReference = luckPermsProvisionService.grant(reward);
            backendSyncClient.markCompleted(reward.rewardId(), externalReference);
        } catch (Exception exception) {
            backendSyncClient.markFailed(reward.rewardId(), reward.paymentId(), exception.getMessage());
        }
    }
}
