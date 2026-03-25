package com.eletroflow.backend.service;

import com.eletroflow.backend.domain.PaymentEntity;
import com.eletroflow.backend.domain.ProvisionRewardEntity;
import com.eletroflow.backend.domain.UserAccountEntity;
import com.eletroflow.backend.domain.VipPlanEntity;
import com.eletroflow.shared.dto.PaymentResponse;
import com.eletroflow.shared.dto.PaymentStatusResponse;
import com.eletroflow.shared.dto.PendingRewardResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toPaymentResponse(PaymentEntity payment, VipPlanEntity plan) {
        return new PaymentResponse(
                payment.getId(),
                payment.getTxid(),
                payment.getStatus(),
                plan.getId(),
                payment.getAmount(),
                payment.getQrCodeBase64(),
                payment.getCopyPasteCode(),
                payment.getExpiresAt()
        );
    }

    public PendingRewardResponse toPendingRewardResponse(ProvisionRewardEntity reward, PaymentEntity payment, UserAccountEntity user, VipPlanEntity plan) {
        return new PendingRewardResponse(
                reward.getId(),
                payment.getId(),
                user.getDiscordId(),
                user.getMinecraftUuid(),
                user.getMinecraftUsername(),
                plan.getId(),
                reward.getLuckPermsGroup(),
                reward.getDiscordRoleId(),
                reward.getStatus(),
                reward.getCreatedAt()
        );
    }

    public PaymentStatusResponse toPaymentStatusResponse(PaymentEntity payment, ProvisionRewardEntity reward) {
        return new PaymentStatusResponse(
                payment.getId(),
                payment.getStatus(),
                reward == null ? null : reward.getStatus(),
                payment.getTxid()
        );
    }
}
