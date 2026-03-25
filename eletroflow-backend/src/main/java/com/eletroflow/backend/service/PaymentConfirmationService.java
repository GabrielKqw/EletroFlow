package com.eletroflow.backend.service;

import com.eletroflow.backend.domain.PaymentEntity;
import com.eletroflow.backend.domain.ProvisionRewardEntity;
import com.eletroflow.backend.domain.TransactionEntity;
import com.eletroflow.backend.domain.UserAccountEntity;
import com.eletroflow.backend.domain.VipPlanEntity;
import com.eletroflow.backend.repository.PaymentRepository;
import com.eletroflow.backend.repository.ProvisionRewardRepository;
import com.eletroflow.backend.repository.TransactionRepository;
import com.eletroflow.backend.repository.UserAccountRepository;
import com.eletroflow.backend.repository.VipPlanRepository;
import com.eletroflow.shared.dto.PaymentWebhookRequest;
import com.eletroflow.shared.enums.PaymentStatus;
import com.eletroflow.shared.enums.ProvisionStatus;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentConfirmationService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final ProvisionRewardRepository provisionRewardRepository;
    private final UserAccountRepository userAccountRepository;
    private final VipPlanRepository vipPlanRepository;
    private final AuditLogService auditLogService;

    public PaymentConfirmationService(
            PaymentRepository paymentRepository,
            TransactionRepository transactionRepository,
            ProvisionRewardRepository provisionRewardRepository,
            UserAccountRepository userAccountRepository,
            VipPlanRepository vipPlanRepository,
            AuditLogService auditLogService
    ) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.provisionRewardRepository = provisionRewardRepository;
        this.userAccountRepository = userAccountRepository;
        this.vipPlanRepository = vipPlanRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void confirmPayment(PaymentWebhookRequest request) {
        if (transactionRepository.findByProviderEventId(request.endToEndId()).isPresent()) {
            return;
        }
        PaymentEntity payment = paymentRepository.findByTxid(request.txid())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for txid"));
        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            return;
        }
        payment.setStatus(PaymentStatus.CONFIRMED);
        payment.setConfirmedAt(request.paidAt());
        payment.setUpdatedAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        TransactionEntity transaction = new TransactionEntity();
        transaction.setPaymentId(payment.getId());
        transaction.setProviderEventId(request.endToEndId());
        transaction.setAmount(request.amount());
        transaction.setPayerDocument(request.payerDocument());
        transaction.setPaidAt(request.paidAt());
        transaction.setCreatedAt(OffsetDateTime.now());
        transactionRepository.save(transaction);

        provisionRewardRepository.findByPaymentId(payment.getId()).orElseGet(() -> createReward(payment));
        auditLogService.log("PAYMENT", payment.getId(), "PAYMENT_CONFIRMED", "txid=" + payment.getTxid());
    }

    private ProvisionRewardEntity createReward(PaymentEntity payment) {
        UserAccountEntity user = userAccountRepository.findById(payment.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        VipPlanEntity plan = vipPlanRepository.findById(payment.getPlanId())
                .orElseThrow(() -> new IllegalStateException("Plan not found"));
        ProvisionRewardEntity reward = new ProvisionRewardEntity();
        reward.setPaymentId(payment.getId());
        reward.setUserId(user.getId());
        reward.setPlanId(plan.getId());
        reward.setLuckPermsGroup(plan.getLuckPermsGroup());
        reward.setDiscordRoleId(plan.getDiscordRoleId());
        reward.setStatus(ProvisionStatus.PENDING);
        reward.setCreatedAt(OffsetDateTime.now());
        reward.setUpdatedAt(OffsetDateTime.now());
        return provisionRewardRepository.save(reward);
    }
}
