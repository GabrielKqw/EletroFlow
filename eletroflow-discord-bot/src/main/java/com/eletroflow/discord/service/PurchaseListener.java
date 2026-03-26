package com.eletroflow.discord.service;

import com.eletroflow.discord.config.BotConfiguration;
import com.eletroflow.shared.dto.CreatePaymentRequest;
import com.eletroflow.shared.dto.PaymentResponse;
import com.eletroflow.shared.dto.VipPlanResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.jetbrains.annotations.NotNull;

public class PurchaseListener extends ListenerAdapter {

    private static final String OPEN_TICKET_BUTTON = "open-ticket-thread";
    private static final String BUY_PREFIX = "buy:";
    private static final String PURCHASE_PREFIX = "purchase:";
    private static final String OWNER_MARKER = "ticket-owner-discord-id:";

    private final BotConfiguration configuration;
    private final BackendClient backendClient;
    private final ActivePaymentRegistry activePaymentRegistry;

    public PurchaseListener(
            BotConfiguration configuration,
            BackendClient backendClient,
            ActivePaymentRegistry activePaymentRegistry
    ) {
        this.configuration = configuration;
        this.backendClient = backendClient;
        this.activePaymentRegistry = activePaymentRegistry;
    }

    public void registerCommands(JDA jda) {
        jda.upsertCommand(Commands.slash("vip-panel", "Publica o painel de compras VIP")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)))
                .queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("vip-panel")) {
            publishPanel(event);
            return;
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getComponentId().equals(OPEN_TICKET_BUTTON)) {
            openPrivatePurchaseThread(event);
            return;
        }
        if (!event.getComponentId().startsWith(BUY_PREFIX)) {
            return;
        }
        Map<String, VipPlanResponse> plansByKey = plansByKey();
        String planKey = event.getComponentId().substring(BUY_PREFIX.length());
        if (!plansByKey.containsKey(planKey)) {
            event.reply("Esse VIP nao esta mais disponivel no momento.").setEphemeral(true).queue();
            return;
        }
        if (!event.getChannelType().isThread() || !isThreadTicketOwner(event.getChannel().asThreadChannel(), event.getUser().getId())) {
            event.reply("Somente o dono do atendimento pode escolher um VIP aqui.").setEphemeral(true).queue();
            return;
        }
        Modal modal = Modal.create(PURCHASE_PREFIX + planKey, "Vincular conta Minecraft")
                .addComponents(
                        ActionRow.of(TextInput.create("uuid", "UUID do Minecraft", TextInputStyle.SHORT).setRequired(true).build()),
                        ActionRow.of(TextInput.create("username", "Nick do Minecraft", TextInputStyle.SHORT).setRequired(true).build())
                )
                .build();
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().startsWith(PURCHASE_PREFIX)) {
            return;
        }
        Map<String, VipPlanResponse> plansByKey = plansByKey();
        String planKey = event.getModalId().substring(PURCHASE_PREFIX.length());
        if (!plansByKey.containsKey(planKey)) {
            event.reply("Esse VIP nao esta mais disponivel no momento.").setEphemeral(true).queue();
            return;
        }
        if (!event.getChannelType().isThread() || !isThreadTicketOwner(event.getChannel().asThreadChannel(), event.getUser().getId())) {
            event.reply("Somente o dono do atendimento pode concluir essa compra.").setEphemeral(true).queue();
            return;
        }
        String uuid = event.getValue("uuid").getAsString();
        String username = event.getValue("username").getAsString();
        try {
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
                    .setTitle("Pix gerado")
                    .setDescription("Use o codigo Pix abaixo. Se ja existia uma cobranca pendente para este VIP, ela foi reaproveitada.")
                    .addField("VIP escolhido", plansByKey.get(planKey).displayName(), false)
                    .addField("Txid", paymentResponse.txid(), false)
                    .addField("Codigo copia e cola", paymentResponse.copyPasteCode(), false);
            event.replyEmbeds(embed.build()).queue();
        } catch (RuntimeException exception) {
            event.reply("Nao consegui gerar a cobranca Pix agora. Tente novamente em alguns minutos.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void publishPanel(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("Esse comando precisa ser usado dentro do servidor.").setEphemeral(true).queue();
            return;
        }
        TextChannel panelChannel = guild.getTextChannelById(configuration.ticketPanelChannelId());
        if (panelChannel == null) {
            event.reply("O canal do painel VIP nao foi configurado corretamente.").setEphemeral(true).queue();
            return;
        }
        panelChannel.sendMessage(new MessageCreateBuilder()
                        .setEmbeds(new EmbedBuilder()
                                .setTitle("Painel de Compras VIP")
                                .setDescription("Clique no botao abaixo para abrir seu atendimento privado de compra.")
                                .build())
                        .setComponents(ActionRow.of(Button.primary(OPEN_TICKET_BUTTON, "Abrir atendimento VIP")))
                        .build())
                .queue();
        event.reply("Painel VIP enviado em " + panelChannel.getAsMention()).setEphemeral(true).queue();
    }

    private void openPrivatePurchaseThread(ButtonInteractionEvent event) {
        if (!(event.getChannel() instanceof TextChannel textChannel)) {
            event.reply("Esse botao so pode ser usado no canal configurado para o painel VIP.").setEphemeral(true).queue();
            return;
        }
        ThreadChannel existingTicket = findExistingTicket(textChannel, event.getUser().getId());
        if (existingTicket != null) {
            event.reply("Voce ja possui um atendimento VIP aberto: " + existingTicket.getAsMention()).setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue();
        textChannel.createThreadChannel(ticketName(event.getUser().getName()), true)
                .queue(threadChannel -> configureNewTicketThread(event, threadChannel),
                        failure -> event.getHook().sendMessage("Nao consegui abrir seu atendimento privado agora. Tente novamente em instantes.").queue());
    }

    private List<Button> planButtons() {
        List<Button> buttons = new ArrayList<>();
        backendClient.listVipPlans().forEach(plan -> buttons.add(Button.primary("buy:" + plan.key(), plan.displayName())));
        return buttons;
    }

    private Map<String, VipPlanResponse> plansByKey() {
        return backendClient.listVipPlans().stream()
                .collect(java.util.stream.Collectors.toMap(VipPlanResponse::key, Function.identity()));
    }

    private void configureNewTicketThread(ButtonInteractionEvent event, ThreadChannel threadChannel) {
        threadChannel.getManager().setInvitable(false).queue();
        threadChannel.addThreadMember(event.getUser()).queue();
        String supportMention = configuration.supportRoleId() != null && !configuration.supportRoleId().isBlank() && !configuration.supportRoleId().equals("0")
                ? "<@&" + configuration.supportRoleId() + ">\n"
                : "";
        threadChannel.sendMessage(new MessageCreateBuilder()
                        .setContent(supportMention + OWNER_MARKER + event.getUser().getId())
                        .setComponents(ActionRow.of(planButtons()))
                        .build())
                .queue(message -> {
                    message.pin().queue();
                    event.getHook().sendMessage("Seu atendimento VIP foi aberto em " + threadChannel.getAsMention()).queue();
                });
    }

    private ThreadChannel findExistingTicket(TextChannel panelChannel, String discordUserId) {
        return panelChannel.getThreadChannels().stream()
                .filter(channel -> isThreadTicketOwner(channel, discordUserId))
                .findFirst()
                .orElse(null);
    }

    private boolean isThreadTicketOwner(ThreadChannel channel, String discordUserId) {
        List<Message> history = channel.getHistory().retrievePast(5).complete();
        return history.stream()
                .anyMatch(message -> message.getAuthor().getId().equals(channel.getJDA().getSelfUser().getId())
                        && message.getContentRaw().contains(OWNER_MARKER + discordUserId));
    }

    private String ticketName(String discordName) {
        String sanitized = discordName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        return "vip-" + sanitized;
    }
}
