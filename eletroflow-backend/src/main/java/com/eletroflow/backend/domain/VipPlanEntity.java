package com.eletroflow.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vip_plans")
public class VipPlanEntity {

    @Id
    @Column(length = 60)
    private String id;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "luckperms_group", nullable = false, length = 80)
    private String luckPermsGroup;

    @Column(name = "discord_role_id", length = 40)
    private String discordRoleId;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getLuckPermsGroup() {
        return luckPermsGroup;
    }

    public void setLuckPermsGroup(String luckPermsGroup) {
        this.luckPermsGroup = luckPermsGroup;
    }

    public String getDiscordRoleId() {
        return discordRoleId;
    }

    public void setDiscordRoleId(String discordRoleId) {
        this.discordRoleId = discordRoleId;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
