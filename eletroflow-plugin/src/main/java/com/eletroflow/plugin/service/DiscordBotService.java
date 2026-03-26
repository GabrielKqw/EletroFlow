package com.eletroflow.plugin.service;

import com.eletroflow.plugin.config.DiscordSettings;
import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.model.PlanRecord;
import com.eletroflow.plugin.storage.PlanRepository;
import java.util.List;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

public class DiscordBotService extends ListenerAdapter {

    private static final String PANEL_BUTTON = "vip-panel-open-thread";
    private static final String SELECT_MENU = "vip-plan-select";
    private static final String OWNER_MARKER = "ticket-owner-discord-id:";
    private static final String MODAL_PREFIX = "vip-modal:";

    private final DiscordSettings settings;
    private final PlanRepository planRepository;
    private final PaymentService paymentService;
    private JDA jda;

    public DiscordBotService(DiscordSettings settings, PlanRepository planRepository, PaymentService paymentService) {
        this.settings = settings;
        this.planRepository = planRepository;
        this.paymentService = paymentService;
    }

    public void start() throws Exception {
        jda = JDABuilder.createDefault(settings.token())
                .addEventListeners(this)
                .build()
                .awaitReady();
        jda.upsertCommand(Commands.slash("vip-panel", "Publica o painel de compras VIP")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)))
                .queue();
    }

    public void stop() {
        if (jda != null) {
            jda.shutdownNow();
        }
    }

    public void notifyApprovedPayment(PaymentRecord payment, PlanRecord plan) {
        if (jda == null) {
            return;
        }
        ThreadChannel threadChannel = jda.getThreadChannelById(payment.discordThreadId());
        if (threadChannel != null) {
            threadChannel.sendMessage("Pagamento confirmado. Seu VIP " + plan.displayName() + " foi entregue.").queue();
        }
        Guild guild = jda.getGuildById(settings.guildId());
        if (guild == null) {
            return;
        }
        String roleId = plan.discordRoleId();
        if (roleId == null || roleId.isBlank() || roleId.equals("0")) {
            return;
        }
        Member member = guild.retrieveMemberById(payment.discordId()).complete();
        Role role = guild.getRoleById(roleId);
        if (member != null && role != null) {
            guild.addRoleToMember(member, role).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!"vip-panel".equals(event.getName())) {
            return;
        }
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("Esse comando precisa ser usado dentro do servidor.").setEphemeral(true).queue();
            return;
        }
        TextChannel panelChannel = guild.getTextChannelById(settings.panelChannelId());
        if (panelChannel == null) {
            event.reply("O canal do painel VIP nao foi configurado corretamente.").setEphemeral(true).queue();
            return;
        }
        panelChannel.sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("Painel de Compras VIP")
                        .setDescription("Clique no botao abaixo para abrir seu atendimento privado.")
                        .build())
                .setComponents(ActionRow.of(Button.primary(PANEL_BUTTON, "Abrir atendimento VIP")))
                .queue();
        event.reply("Painel VIP enviado em " + panelChannel.getAsMention()).setEphemeral(true).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!PANEL_BUTTON.equals(event.getComponentId())) {
            return;
        }
        if (!(event.getChannel() instanceof TextChannel textChannel)) {
            event.reply("Esse botao so pode ser usado no canal do painel VIP.").setEphemeral(true).queue();
            return;
        }
        ThreadChannel existing = findExistingThread(textChannel, event.getUser().getId());
        if (existing != null) {
            event.reply("Voce ja possui um atendimento VIP aberto: " + existing.getAsMention()).setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue();
        textChannel.createThreadChannel("vip-" + sanitize(event.getUser().getName()), true)
                .setInvitable(false)
                .queue(thread -> {
                    thread.addThreadMember(event.getUser()).queue();
                    sendPlanSelection(thread, event.getUser().getId());
                    event.getHook().sendMessage("Seu atendimento VIP foi aberto em " + thread.getAsMention()).queue();
                }, failure -> event.getHook().sendMessage("Nao consegui abrir seu atendimento agora.").queue());
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!SELECT_MENU.equals(event.getComponentId())) {
            return;
        }
        if (!event.getChannelType().isThread() || !isThreadOwner(event.getChannel().asThreadChannel(), event.getUser().getId())) {
            event.reply("Somente o dono do atendimento pode escolher um VIP aqui.").setEphemeral(true).queue();
            return;
        }
        String planKey = event.getValues().getFirst();
        Modal modal = Modal.create(MODAL_PREFIX + planKey, "Vincular conta Minecraft")
                .addActionRow(TextInput.create("uuid", "UUID do Minecraft", TextInputStyle.SHORT).setRequired(true).build())
                .addActionRow(TextInput.create("username", "Nick do Minecraft", TextInputStyle.SHORT).setRequired(true).build())
                .build();
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().startsWith(MODAL_PREFIX)) {
            return;
        }
        if (!event.getChannelType().isThread() || !isThreadOwner(event.getChannel().asThreadChannel(), event.getUser().getId())) {
            event.reply("Somente o dono do atendimento pode concluir a compra.").setEphemeral(true).queue();
            return;
        }
        String planKey = event.getModalId().substring(MODAL_PREFIX.length());
        try {
            PlanRecord plan = planRepository.findRequiredPlan(planKey);
            PaymentRecord payment = paymentService.createOrReusePayment(
                    event.getUser().getId(),
                    event.getValue("uuid").getAsString(),
                    event.getValue("username").getAsString(),
                    event.getChannel().getId(),
                    plan
            );
            event.replyEmbeds(new EmbedBuilder()
                            .setTitle("Pix gerado")
                            .setDescription("Finalize o pagamento usando o codigo abaixo.")
                            .addField("VIP", plan.displayName(), false)
                            .addField("Valor", payment.amount() + " " + plan.currency(), false)
                            .addField("Codigo copia e cola", payment.copyPasteCode(), false)
                            .build())
                    .queue();
        } catch (IllegalArgumentException exception) {
            event.reply(exception.getMessage()).setEphemeral(true).queue();
        } catch (RuntimeException exception) {
            event.reply("Nao consegui gerar a cobranca Pix agora. Tente novamente em alguns minutos.").setEphemeral(true).queue();
        }
    }

    private void sendPlanSelection(ThreadChannel threadChannel, String discordId) {
        List<SelectOption> options = planRepository.findActivePlans().stream()
                .map(plan -> SelectOption.of(plan.displayName(), plan.key())
                        .withDescription("R$ " + plan.amount() + " por " + plan.durationDays() + " dias"))
                .limit(25)
                .toList();
        String supportMention = settings.supportRoleId() != null && !settings.supportRoleId().isBlank() && !settings.supportRoleId().equals("0")
                ? "<@&" + settings.supportRoleId() + ">\n"
                : "";
        threadChannel.sendMessage(supportMention + OWNER_MARKER + discordId)
                .setComponents(ActionRow.of(StringSelectMenu.create(SELECT_MENU).addOptions(options).setPlaceholder("Selecione o VIP desejado").build()))
                .queue(message -> message.pin().queue());
    }

    private ThreadChannel findExistingThread(TextChannel panelChannel, String discordId) {
        return panelChannel.getThreadChannels().stream()
                .filter(thread -> isThreadOwner(thread, discordId))
                .findFirst()
                .orElse(null);
    }

    private boolean isThreadOwner(ThreadChannel threadChannel, String discordId) {
        List<Message> messages = threadChannel.getHistory().retrievePast(5).complete();
        return messages.stream()
                .anyMatch(message -> message.getAuthor().getId().equals(threadChannel.getJDA().getSelfUser().getId())
                        && message.getContentRaw().contains(OWNER_MARKER + discordId));
    }

    private String sanitize(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
    }
}
