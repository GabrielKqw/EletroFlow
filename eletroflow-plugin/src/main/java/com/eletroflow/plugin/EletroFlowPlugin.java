package com.eletroflow.plugin;

import com.eletroflow.plugin.config.PluginConfigurationLoader;
import com.eletroflow.plugin.config.PluginSettings;
import com.eletroflow.plugin.config.PluginStartupValidator;
import com.eletroflow.plugin.efi.EfiPixClient;
import com.eletroflow.plugin.service.DiscordBotService;
import com.eletroflow.plugin.service.EfiWebhookService;
import com.eletroflow.plugin.service.PaymentConfirmationService;
import com.eletroflow.plugin.service.MinecraftIdentityService;
import com.eletroflow.plugin.service.PaymentPollService;
import com.eletroflow.plugin.service.PaymentService;
import com.eletroflow.plugin.service.ReceiptPdfService;
import com.eletroflow.plugin.storage.AuditLogRepository;
import com.eletroflow.plugin.storage.DatabaseManager;
import com.eletroflow.plugin.storage.PaymentRepository;
import com.eletroflow.plugin.storage.PaymentTransactionRepository;
import com.eletroflow.plugin.storage.PlanRepository;
import com.eletroflow.plugin.storage.SchemaMigrationService;
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
    private EfiWebhookService efiWebhookService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("vip-plans.yml", false);
        PluginConfigurationLoader loader = new PluginConfigurationLoader(this);
        PluginSettings settings = loader.loadSettings();
        var vipPlans = loader.loadVipPlans();
        new PluginStartupValidator().validate(settings, vipPlans);
        getLogger().info("Loaded " + vipPlans.size() + " VIP plans from vip-plans.yml");
        DatabaseManager databaseManager = new DatabaseManager(settings.database());
        validateDatabaseConnection(databaseManager);
        getLogger().info("PostgreSQL connection validated for server " + settings.serverId());
        new SchemaMigrationService(databaseManager).migrate();
        getLogger().info("Schema migrations checked successfully");
        PlanRepository planRepository = new PlanRepository(databaseManager);
        planRepository.sync(vipPlans);
        getLogger().info("VIP catalog synchronized to PostgreSQL");
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
        PaymentConfirmationService paymentConfirmationService = new PaymentConfirmationService(
                paymentRepository,
                paymentTransactionRepository,
                planRepository,
                vipGrantRepository,
                auditLogRepository,
                new LuckPermsProvisionService(luckPerms),
                discordBotService
        );
        try {
            discordBotService.start();
            getLogger().info("Discord integration started");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to start Discord integration", exception);
        }
        efiWebhookService = new EfiWebhookService(this, settings.webhook(), objectMapper, paymentRepository, paymentConfirmationService, efiPixClient);
        efiWebhookService.start();
        paymentPollService = new PaymentPollService(
                this,
                paymentRepository,
                efiPixClient,
                paymentConfirmationService
        );
        paymentPollService.start(settings.syncIntervalTicks());
    }

    @Override
    public void onDisable() {
        if (efiWebhookService != null) {
            efiWebhookService.stop();
        }
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
