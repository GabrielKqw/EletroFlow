package com.eletroflow.discord;

import com.eletroflow.discord.config.BotConfiguration;
import com.eletroflow.discord.config.ConfigurationLoader;
import com.eletroflow.discord.service.ActivePaymentRegistry;
import com.eletroflow.discord.service.BackendClient;
import com.eletroflow.discord.service.PaymentStatusPoller;
import com.eletroflow.discord.service.PurchaseListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public final class EletroFlowDiscordBotApplication {

    private EletroFlowDiscordBotApplication() {
    }

    public static void main(String[] args) throws Exception {
        ConfigurationLoader loader = new ConfigurationLoader();
        BotConfiguration configuration = loader.loadBotConfiguration();
        ActivePaymentRegistry activePaymentRegistry = new ActivePaymentRegistry(loader.stateFile());
        BackendClient backendClient = new BackendClient(configuration, loader.objectMapper());
        PurchaseListener purchaseListener = new PurchaseListener(configuration, backendClient, activePaymentRegistry);
        JDA jda = JDABuilder.createDefault(configuration.token())
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(purchaseListener)
                .build()
                .awaitReady();
        purchaseListener.registerCommands(jda);
        PaymentStatusPoller poller = new PaymentStatusPoller(jda, configuration, backendClient, activePaymentRegistry);
        poller.start();
    }
}
