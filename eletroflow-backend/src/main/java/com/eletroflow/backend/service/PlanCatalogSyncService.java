package com.eletroflow.backend.service;

import com.eletroflow.backend.config.PlanCatalogProperties;
import com.eletroflow.backend.domain.VipPlanEntity;
import com.eletroflow.backend.repository.VipPlanRepository;
import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PlanCatalogSyncService {

    private final PlanCatalogProperties properties;
    private final VipPlanRepository vipPlanRepository;

    public PlanCatalogSyncService(PlanCatalogProperties properties, VipPlanRepository vipPlanRepository) {
        this.properties = properties;
        this.vipPlanRepository = vipPlanRepository;
    }

    @PostConstruct
    public void synchronize() {
        OffsetDateTime now = OffsetDateTime.now();
        Set<String> configuredPlans = new HashSet<>();
        properties.plans().forEach((key, value) -> {
            configuredPlans.add(key);
            VipPlanEntity entity = vipPlanRepository.findById(key).orElseGet(VipPlanEntity::new);
            entity.setId(key);
            entity.setDisplayName(value.displayName());
            entity.setAmount(value.amount());
            entity.setCurrency(value.currency());
            entity.setLuckPermsGroup(value.luckPermsGroup());
            entity.setDiscordRoleId(value.discordRoleId());
            entity.setDurationDays(value.durationDays());
            entity.setActive(true);
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(now);
            }
            entity.setUpdatedAt(now);
            vipPlanRepository.save(entity);
        });
        vipPlanRepository.findAll().stream()
                .filter(entity -> !configuredPlans.contains(entity.getId()))
                .forEach(entity -> {
                    entity.setActive(false);
                    entity.setUpdatedAt(now);
                    vipPlanRepository.save(entity);
                });
    }
}
