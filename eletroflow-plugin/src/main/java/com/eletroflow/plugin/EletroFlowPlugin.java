package com.eletroflow.plugin;

import com.eletroflow.plugin.service.BackendSyncClient;
import com.eletroflow.plugin.service.RewardSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EletroFlowPlugin extends JavaPlugin {

    private RewardSyncService rewardSyncService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("vip-plans.yml", false);
        LuckPerms luckPerms = LuckPermsProvider.get();
        BackendSyncClient backendSyncClient = new BackendSyncClient(getConfig(), new ObjectMapper());
        rewardSyncService = new RewardSyncService(this, backendSyncClient, new LuckPermsProvisionService(luckPerms));
        rewardSyncService.start();
    }

    @Override
    public void onDisable() {
        if (rewardSyncService != null) {
            rewardSyncService.stop();
        }
    }
}
