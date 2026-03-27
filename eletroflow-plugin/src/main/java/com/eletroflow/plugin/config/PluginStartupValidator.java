package com.eletroflow.plugin.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PluginStartupValidator {

    public void validate(PluginSettings settings, List<VipPlanDefinition> vipPlans) {
        List<String> violations = new ArrayList<>();
        require(settings.serverId(), "server.id", violations);
        validateDatabase(settings.database(), violations);
        validateDiscord(settings.discord(), violations);
        validateEfi(settings.efi(), violations);
        validateWebhook(settings.webhook(), violations);
        validatePlans(vipPlans, violations);
        if (!violations.isEmpty()) {
            throw new IllegalStateException(String.join(System.lineSeparator(), violations));
        }
    }

    private void validateDatabase(DatabaseSettings settings, List<String> violations) {
        require(settings.jdbcUrl(), "database.jdbc-url", violations);
        require(settings.username(), "database.username", violations);
        require(settings.password(), "database.password", violations);
    }

    private void validateDiscord(DiscordSettings settings, List<String> violations) {
        require(settings.token(), "discord.token", violations);
        require(settings.guildId(), "discord.guild-id", violations);
        require(settings.panelChannelId(), "discord.panel-channel-id", violations);
    }

    private void validateEfi(EfiSettings settings, List<String> violations) {
        require(settings.baseUrl(), "efi.base-url", violations);
        require(settings.clientId(), "efi.client-id", violations);
        require(settings.clientSecret(), "efi.client-secret", violations);
        require(settings.pixKey(), "efi.pix-key", violations);
        require(settings.receiverName(), "efi.receiver-name", violations);
        require(settings.receiverDocument(), "efi.receiver-document", violations);
        if (settings.receiverDocument() != null && !settings.receiverDocument().isBlank()) {
            String digits = settings.receiverDocument().replaceAll("\\D", "");
            if (digits.length() != 11 && digits.length() != 14) {
                violations.add("efi.receiver-document must contain 11 or 14 digits");
            }
        }
        if (settings.certificatePath() == null || settings.certificatePath().isBlank()) {
            violations.add("Missing required config: efi.certificate-path");
            return;
        }
        if (!Files.exists(Path.of(settings.certificatePath()))) {
            violations.add("EFI certificate file not found at " + settings.certificatePath());
        }
    }

    private void validateWebhook(WebhookSettings settings, List<String> violations) {
        if (!settings.enabled()) {
            return;
        }
        require(settings.bindAddress(), "webhook.bind-address", violations);
        require(settings.path(), "webhook.path", violations);
        require(settings.publicUrl(), "webhook.public-url", violations);
        require(settings.token(), "webhook.token", violations);
        if (settings.port() <= 0 || settings.port() > 65535) {
            violations.add("Invalid webhook.port value: " + settings.port());
        }
        if (settings.path() != null && !settings.path().startsWith("/")) {
            violations.add("webhook.path must start with /");
        }
    }

    private void validatePlans(List<VipPlanDefinition> vipPlans, List<String> violations) {
        if (vipPlans == null || vipPlans.isEmpty()) {
            violations.add("vip-plans.yml must define at least one VIP plan");
            return;
        }
        for (VipPlanDefinition plan : vipPlans) {
            require(plan.key(), "vip-plans." + plan.key() + ".key", violations);
            require(plan.displayName(), "vip-plans." + plan.key() + ".display-name", violations);
            require(plan.currency(), "vip-plans." + plan.key() + ".currency", violations);
            require(plan.luckPermsGroup(), "vip-plans." + plan.key() + ".luckperms-group", violations);
            if (plan.amount() == null || plan.amount().signum() <= 0) {
                violations.add("VIP plan " + plan.key() + " must have a positive amount");
            }
            if (plan.durationDays() <= 0) {
                violations.add("VIP plan " + plan.key() + " must have duration-days greater than zero");
            }
        }
    }

    private void require(String value, String field, List<String> violations) {
        if (value == null || value.isBlank() || "change-me".equalsIgnoreCase(value) || "\"\"".equals(value)) {
            violations.add("Missing required config: " + field);
        }
    }
}
