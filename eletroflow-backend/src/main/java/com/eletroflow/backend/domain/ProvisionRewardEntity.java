package com.eletroflow.backend.domain;

import com.eletroflow.shared.enums.ProvisionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "provision_rewards")
public class ProvisionRewardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "plan_id", nullable = false)
    private String planId;

    @Column(name = "luckperms_group", nullable = false, length = 80)
    private String luckPermsGroup;

    @Column(name = "discord_role_id", length = 40)
    private String discordRoleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProvisionStatus status;

    @Column(name = "assigned_server_id", length = 100)
    private String assignedServerId;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public String getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
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

    public ProvisionStatus getStatus() {
        return status;
    }

    public void setStatus(ProvisionStatus status) {
        this.status = status;
    }

    public String getAssignedServerId() {
        return assignedServerId;
    }

    public void setAssignedServerId(String assignedServerId) {
        this.assignedServerId = assignedServerId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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
