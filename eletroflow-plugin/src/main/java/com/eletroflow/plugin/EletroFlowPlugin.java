package com.eletroflow.plugin;

import com.eletroflow.plugin.config.PluginConfigurationLoader;
import com.eletroflow.plugin.config.PluginSettings;
import com.eletroflow.plugin.efi.EfiPixClient;
import com.eletroflow.plugin.service.DiscordBotService;
import com.eletroflow.plugin.service.MinecraftIdentityService;
import com.eletroflow.plugin.service.PaymentPollService;
import com.eletroflow.plugin.service.PaymentService;
import com.eletroflow.plugin.service.ReceiptPdfService;
import com.eletroflow.plugin.storage.AuditLogRepository;
import com.eletroflow.plugin.storage.DatabaseManager;
import com.eletroflow.plugin.storage.PaymentRepository;
import com.eletroflow.plugin.storage.PaymentTransactionRepository;
import com.eletroflow.plugin.storage.PlanRepository;
import com.eletroflow.plugin.storage.UserRepository;
import com.eletroflow.plugin.storage.VipGrantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
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
        validateDatabaseConnection(databaseManager);
        PlanRepository planRepository = new PlanRepository(databaseManager);
        planRepository.sync(loader.loadVipPlans());
        LuckPerms luckPerms = LuckPermsProvider.get();
        PaymentRepository paymentRepository = new PaymentRepository(databaseManager);
        UserRepository userRepository = new UserRepository(databaseManager);
        PaymentTransactionRepository paymentTransactionRepository = new PaymentTransactionRepository(databaseManager);
        VipGrantRepository vipGrantRepository = new VipGrantRepository(databaseManager);
        AuditLogRepository auditLogRepository = new AuditLogRepository(databaseManager);
        ObjectMapper objectMapper = new ObjectMapper();
        EfiPixClient efiPixClient = new EfiPixClient(objectMapper, settings.efi());
        MinecraftIdentityService minecraftIdentityService = new MinecraftIdentityService(settings.minecraft(), objectMapper);
        ReceiptPdfService receiptPdfService = new ReceiptPdfService(settings.efi());
        PaymentService paymentService = new PaymentService(
                paymentRepository,
                userRepository,
                auditLogRepository,
                minecraftIdentityService,
                efiPixClient
        );
        discordBotService = new DiscordBotService(settings.discord(), planRepository, paymentService, receiptPdfService);
        try {
            discordBotService.start();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to start Discord integration", exception);
        }
        paymentPollService = new PaymentPollService(
                this,
                paymentRepository,
                planRepository,
                paymentTransactionRepository,
                vipGrantRepository,
                auditLogRepository,
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

    private void validateDatabaseConnection(DatabaseManager databaseManager) {
        try (Connection ignored = databaseManager.getConnection()) {
        } catch (Exception exception) {
            String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            throw new IllegalStateException("Failed to connect to PostgreSQL: " + reason, exception);
        }
    }
}
