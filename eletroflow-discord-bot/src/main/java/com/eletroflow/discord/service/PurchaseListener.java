package com.eletroflow.discord.service;

import com.eletroflow.discord.config.BotConfiguration;
import com.eletroflow.discord.config.PlanCatalog;
import com.eletroflow.shared.dto.CreatePaymentRequest;
import com.eletroflow.shared.dto.PaymentResponse;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.jetbrains.annotations.NotNull;

public class PurchaseListener extends ListenerAdapter {

    private final BotConfiguration configuration;
    private final PlanCatalog planCatalog;
    private final BackendClient backendClient;
    private final ActivePaymentRegistry activePaymentRegistry;

    public PurchaseListener(
            BotConfiguration configuration,
            PlanCatalog planCatalog,
            BackendClient backendClient,
            ActivePaymentRegistry activePaymentRegistry
    ) {
        this.configuration = configuration;
        this.planCatalog = planCatalog;
        this.backendClient = backendClient;
        this.activePaymentRegistry = activePaymentRegistry;
    }

    public void registerCommands(JDA jda) {
        jda.upsertCommand(Commands.slash("ticket", "Open a VIP purchase ticket"))
                .queue();
        jda.upsertCommand(Commands.slash("vip-link", "Link a Minecraft account")
                        .addOption(OptionType.STRING, "uuid", "Minecraft UUID", true)
                        .addOption(OptionType.STRING, "username", "Minecraft username", true))
                .queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("ticket")) {
            createTicket(event);
            return;
        }
        if (event.getName().equals("vip-link")) {
            event.reply("Use the purchase flow inside your ticket so the account is linked to the payment.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("buy:")) {
            String planKey = event.getComponentId().substring("buy:".length());
            Modal modal = Modal.create("purchase:" + planKey, "Minecraft Account")
                    .addComponents(
                            ActionRow.of(TextInput.create("uuid", "Minecraft UUID", TextInputStyle.SHORT).setRequired(true).build()),
                            ActionRow.of(TextInput.create("username", "Minecraft Username", TextInputStyle.SHORT).setRequired(true).build())
                    )
                    .build();
            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().startsWith("purchase:")) {
            return;
        }
        String planKey = event.getModalId().substring("purchase:".length());
        String uuid = event.getValue("uuid").getAsString();
        String username = event.getValue("username").getAsString();
        PaymentResponse paymentResponse = backendClient.createPayment(new CreatePaymentRequest(
                event.getUser().getId(),
                planKey,
                uuid,
                username,
                event.getChannel().getId()
        ));
        activePaymentRegistry.put(new ActivePaymentRegistry.TrackedPayment(
                paymentResponse.paymentId(),
                planKey,
                event.getUser().getId(),
                event.getChannel().getId()
        ));
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Pix generated")
                .setDescription("Copy and pay the Pix code below to receive your VIP automatically.")
                .addField("Plan", planCatalog.plans().get(planKey).displayName(), false)
                .addField("Txid", paymentResponse.txid(), false)
                .addField("Copy and Paste", paymentResponse.copyPasteCode(), false);
        event.replyEmbeds(embed.build()).queue();
    }

    private void createTicket(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("Guild context is required.").setEphemeral(true).queue();
            return;
        }
        Category category = guild.getCategoryById(configuration.ticketCategoryId());
        Member member = event.getMember();
        if (category == null || member == null) {
            event.reply("Ticket configuration is invalid.").setEphemeral(true).queue();
            return;
        }
        ChannelAction<TextChannel> action = guild.createTextChannel("vip-" + event.getUser().getName(), category);
        action.addPermissionOverride(member, List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), List.of());
        if (configuration.supportRoleId() != null && !configuration.supportRoleId().isBlank()) {
            action.addRolePermissionOverride(Long.parseLong(configuration.supportRoleId()), List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), List.of());
        }
        action.queue(channel -> {
            channel.sendMessage(new MessageCreateBuilder()
                            .setContent("Choose your VIP plan below.")
                            .setComponents(ActionRow.of(planButtons()))
                            .build())
                    .queue();
            event.reply("Ticket created: " + channel.getAsMention()).setEphemeral(true).queue();
        }, failure -> event.reply("Failed to create ticket.").setEphemeral(true).queue());
    }

    private List<Button> planButtons() {
        List<Button> buttons = new ArrayList<>();
        planCatalog.plans().forEach((key, plan) -> buttons.add(Button.primary("buy:" + key, plan.displayName())));
        return buttons;
    }
}
