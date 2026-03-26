package com.eletroflow.plugin;

import com.eletroflow.plugin.config.PluginConfigurationLoader;
import com.eletroflow.plugin.config.PluginSettings;
import com.eletroflow.plugin.efi.EfiPixClient;
import com.eletroflow.plugin.service.DiscordBotService;
import com.eletroflow.plugin.service.PaymentPollService;
import com.eletroflow.plugin.service.PaymentService;
import com.eletroflow.plugin.storage.DatabaseManager;
import com.eletroflow.plugin.storage.PaymentRepository;
import com.eletroflow.plugin.storage.PlanRepository;
import com.eletroflow.plugin.storage.SchemaInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EletroFlowPlugin extends JavaPlugin {

    private DiscordBotService discordBotService;
    private PaymentPollService paymentPollService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("vip-plans.yml", false);
        PluginConfigurationLoader loader = new PluginConfigurationLoader(this);
        PluginSettings settings = loader.loadSettings();
        DatabaseManager databaseManager = new DatabaseManager(settings.database());
        SchemaInitializer schemaInitializer = new SchemaInitializer(databaseManager);
        schemaInitializer.initialize();
        PlanRepository planRepository = new PlanRepository(databaseManager);
        planRepository.sync(loader.loadVipPlans());
        LuckPerms luckPerms = LuckPermsProvider.get();
        PaymentRepository paymentRepository = new PaymentRepository(databaseManager);
        EfiPixClient efiPixClient = new EfiPixClient(new ObjectMapper(), settings.efi());
        PaymentService paymentService = new PaymentService(paymentRepository, efiPixClient);
        discordBotService = new DiscordBotService(settings.discord(), planRepository, paymentService);
        try {
            discordBotService.start();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to start Discord integration", exception);
        }
        paymentPollService = new PaymentPollService(
                this,
                paymentRepository,
                planRepository,
                efiPixClient,
                new LuckPermsProvisionService(luckPerms),
                discordBotService
        );
        paymentPollService.start(settings.syncIntervalTicks());
    }

    @Override
    public void onDisable() {
        if (paymentPollService != null) {
            paymentPollService.stop();
        }
        if (discordBotService != null) {
            discordBotService.stop();
        }
    }
}
