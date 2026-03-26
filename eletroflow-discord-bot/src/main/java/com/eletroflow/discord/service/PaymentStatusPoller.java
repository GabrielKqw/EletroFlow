package com.eletroflow.discord.service;

import com.eletroflow.discord.config.BotConfiguration;
import com.eletroflow.shared.enums.PaymentStatus;
import com.eletroflow.shared.dto.VipPlanResponse;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentStatusPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentStatusPoller.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final JDA jda;
    private final BotConfiguration configuration;
    private final BackendClient backendClient;
    private final ActivePaymentRegistry activePaymentRegistry;

    public PaymentStatusPoller(
            JDA jda,
            BotConfiguration configuration,
            BackendClient backendClient,
            ActivePaymentRegistry activePaymentRegistry
    ) {
        this.jda = jda;
        this.configuration = configuration;
        this.backendClient = backendClient;
        this.activePaymentRegistry = activePaymentRegistry;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::poll, 10, configuration.paymentPollIntervalSeconds(), TimeUnit.SECONDS);
    }

    private void poll() {
        Map<String, VipPlanResponse> plansByKey = backendClient.listVipPlans().stream()
                .collect(java.util.stream.Collectors.toMap(VipPlanResponse::key, Function.identity()));
        for (ActivePaymentRegistry.TrackedPayment trackedPayment : activePaymentRegistry.all()) {
            try {
                if (backendClient.getPaymentStatus(trackedPayment.paymentId()).paymentStatus() != PaymentStatus.CONFIRMED) {
                    continue;
                }
                TextChannel channel = jda.getTextChannelById(trackedPayment.ticketChannelId());
                Guild guild = jda.getGuildById(configuration.guildId());
                if (channel != null) {
                    channel.sendMessage("Pagamento confirmado. Seu VIP agora entrou na fila de entrega.").queue();
                }
                if (guild != null) {
                    Member member = guild.retrieveMemberById(trackedPayment.discordUserId()).complete();
                    VipPlanResponse plan = plansByKey.get(trackedPayment.planKey());
                    String roleId = plan == null ? null : plan.discordRoleId();
                    if (member != null && roleId != null && !roleId.isBlank() && !roleId.equals("0")) {
                        Role role = guild.getRoleById(roleId);
                        if (role != null) {
                            guild.addRoleToMember(member, role).queue();
                        }
                    }
                }
                activePaymentRegistry.remove(trackedPayment.paymentId());
            } catch (Exception exception) {
                LOGGER.warn("Failed to process tracked payment {}", trackedPayment.paymentId(), exception);
            }
        }
    }
}
