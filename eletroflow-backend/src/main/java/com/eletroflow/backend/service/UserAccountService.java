package com.eletroflow.backend.service;

import com.eletroflow.backend.domain.UserAccountEntity;
import com.eletroflow.backend.repository.UserAccountRepository;
import com.eletroflow.shared.dto.LinkMinecraftAccountRequest;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;

    public UserAccountService(UserAccountRepository userAccountRepository, AuditLogService auditLogService) {
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UserAccountEntity upsertDiscordUser(String discordId, String minecraftUuid, String minecraftUsername) {
        OffsetDateTime now = OffsetDateTime.now();
        UserAccountEntity entity = userAccountRepository.findByDiscordId(discordId).orElseGet(UserAccountEntity::new);
        entity.setDiscordId(discordId);
        entity.setMinecraftUuid(minecraftUuid);
        entity.setMinecraftUsername(minecraftUsername);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        UserAccountEntity saved = userAccountRepository.save(entity);
        auditLogService.log("USER", saved.getId(), "UPSERT_LINK", "discordId=" + discordId);
        return saved;
    }

    @Transactional
    public UserAccountEntity linkAccount(LinkMinecraftAccountRequest request) {
        return upsertDiscordUser(request.discordId(), request.minecraftUuid(), request.minecraftUsername());
    }
}
