package com.eletroflow.backend.service;

import com.eletroflow.backend.domain.PaymentEntity;
import com.eletroflow.backend.domain.ProvisionRewardEntity;
import com.eletroflow.backend.domain.UserAccountEntity;
import com.eletroflow.backend.domain.VipPlanEntity;
import com.eletroflow.backend.repository.PaymentRepository;
import com.eletroflow.backend.repository.ProvisionRewardRepository;
import com.eletroflow.backend.repository.UserAccountRepository;
import com.eletroflow.backend.repository.VipPlanRepository;
import com.eletroflow.shared.dto.PendingRewardResponse;
import com.eletroflow.shared.dto.RewardAckRequest;
import com.eletroflow.shared.enums.ProvisionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RewardDispatchService {

    private final ProvisionRewardRepository provisionRewardRepository;
    private final PaymentRepository paymentRepository;
    private final UserAccountRepository userAccountRepository;
    private final VipPlanRepository vipPlanRepository;
    private final PaymentMapper paymentMapper;
    private final AuditLogService auditLogService;

    public RewardDispatchService(
            ProvisionRewardRepository provisionRewardRepository,
            PaymentRepository paymentRepository,
            UserAccountRepository userAccountRepository,
            VipPlanRepository vipPlanRepository,
            PaymentMapper paymentMapper,
            AuditLogService auditLogService
    ) {
        this.provisionRewardRepository = provisionRewardRepository;
        this.paymentRepository = paymentRepository;
        this.userAccountRepository = userAccountRepository;
        this.vipPlanRepository = vipPlanRepository;
        this.paymentMapper = paymentMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public List<PendingRewardResponse> claimPendingRewards() {
        return provisionRewardRepository.findTop20ByStatusOrderByCreatedAtAsc(ProvisionStatus.PENDING).stream()
                .map(reward -> {
                    reward.setStatus(ProvisionStatus.PROCESSING);
                    reward.setUpdatedAt(OffsetDateTime.now());
                    ProvisionRewardEntity saved = provisionRewardRepository.save(reward);
                    PaymentEntity payment = paymentRepository.findById(saved.getPaymentId())
                            .orElseThrow(() -> new IllegalStateException("Payment missing"));
                    UserAccountEntity user = userAccountRepository.findById(saved.getUserId())
                            .orElseThrow(() -> new IllegalStateException("User missing"));
                    VipPlanEntity plan = vipPlanRepository.findById(saved.getPlanId())
                            .orElseThrow(() -> new IllegalStateException("Plan missing"));
                    return paymentMapper.toPendingRewardResponse(saved, payment, user, plan);
                })
                .toList();
    }

    @Transactional
    public void markCompleted(String rewardId, RewardAckRequest request) {
        ProvisionRewardEntity reward = provisionRewardRepository.findById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));
        reward.setStatus(ProvisionStatus.COMPLETED);
        reward.setAssignedServerId(request.serverId());
        reward.setExternalReference(request.externalReference());
        reward.setFailureReason(null);
        reward.setUpdatedAt(OffsetDateTime.now());
        provisionRewardRepository.save(reward);
        auditLogService.log("REWARD", reward.getId(), "REWARD_COMPLETED", "serverId=" + request.serverId());
    }

    @Transactional
    public void markFailed(String rewardId, RewardAckRequest request) {
        ProvisionRewardEntity reward = provisionRewardRepository.findById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));
        reward.setStatus(ProvisionStatus.FAILED);
        reward.setAssignedServerId(request.serverId());
        reward.setExternalReference(request.externalReference());
        reward.setFailureReason(request.failureReason());
        reward.setUpdatedAt(OffsetDateTime.now());
        provisionRewardRepository.save(reward);
        auditLogService.log("REWARD", reward.getId(), "REWARD_FAILED", request.failureReason());
    }
}
