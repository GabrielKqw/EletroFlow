package com.eletroflow.discord.service;

import com.eletroflow.discord.config.BotConfiguration;
import com.eletroflow.discord.config.PlanCatalog;
import com.eletroflow.shared.enums.PaymentStatus;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class PaymentStatusPoller {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final JDA jda;
    private final BotConfiguration configuration;
    private final PlanCatalog planCatalog;
    private final BackendClient backendClient;
    private final ActivePaymentRegistry activePaymentRegistry;

    public PaymentStatusPoller(
            JDA jda,
            BotConfiguration configuration,
            PlanCatalog planCatalog,
            BackendClient backendClient,
            ActivePaymentRegistry activePaymentRegistry
    ) {
        this.jda = jda;
        this.configuration = configuration;
        this.planCatalog = planCatalog;
        this.backendClient = backendClient;
        this.activePaymentRegistry = activePaymentRegistry;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::poll, 10, configuration.paymentPollIntervalSeconds(), TimeUnit.SECONDS);
    }

    private void poll() {
        for (ActivePaymentRegistry.TrackedPayment trackedPayment : activePaymentRegistry.all()) {
            try {
                if (backendClient.getPaymentStatus(trackedPayment.paymentId()).paymentStatus() != PaymentStatus.CONFIRMED) {
                    continue;
                }
                TextChannel channel = jda.getTextChannelById(trackedPayment.ticketChannelId());
                Guild guild = jda.getGuildById(configuration.guildId());
                if (channel != null) {
                    channel.sendMessage("Payment confirmed. Your VIP provisioning is now in progress.").queue();
                }
                if (guild != null) {
                    Member member = guild.retrieveMemberById(trackedPayment.discordUserId()).complete();
                    String roleId = planCatalog.plans().get(trackedPayment.planKey()).discordRoleId();
                    if (member != null && roleId != null && !roleId.isBlank() && !roleId.equals("0")) {
                        Role role = guild.getRoleById(roleId);
                        if (role != null) {
                            guild.addRoleToMember(member, role).queue();
                        }
                    }
                }
                activePaymentRegistry.remove(trackedPayment.paymentId());
            } catch (Exception ignored) {
            }
        }
    }
}
