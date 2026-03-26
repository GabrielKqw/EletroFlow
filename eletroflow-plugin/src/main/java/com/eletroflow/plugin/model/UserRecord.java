package com.eletroflow.plugin.model;

public record UserRecord(
        String id,
        String discordId,
        String minecraftUuid,
        String minecraftUsername
) {
}

