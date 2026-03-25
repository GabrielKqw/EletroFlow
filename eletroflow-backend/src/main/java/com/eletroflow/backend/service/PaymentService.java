package com.eletroflow.backend.service;

import com.eletroflow.backend.domain.PaymentEntity;
import com.eletroflow.backend.domain.ProvisionRewardEntity;
import com.eletroflow.backend.domain.UserAccountEntity;
import com.eletroflow.backend.domain.VipPlanEntity;
import com.eletroflow.backend.efi.EfiPixClient;
import com.eletroflow.backend.repository.PaymentRepository;
import com.eletroflow.backend.repository.ProvisionRewardRepository;
import com.eletroflow.backend.repository.VipPlanRepository;
import com.eletroflow.shared.dto.CreatePaymentRequest;
import com.eletroflow.shared.dto.PaymentResponse;
import com.eletroflow.shared.dto.PaymentStatusResponse;
import com.eletroflow.shared.enums.PaymentStatus;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final VipPlanRepository vipPlanRepository;
    private final PaymentRepository paymentRepository;
    private final ProvisionRewardRepository provisionRewardRepository;
    private final UserAccountService userAccountService;
    private final EfiPixClient efiPixClient;
    private final PaymentMapper paymentMapper;
    private final AuditLogService auditLogService;

    public PaymentService(
            VipPlanRepository vipPlanRepository,
            PaymentRepository paymentRepository,
            ProvisionRewardRepository provisionRewardRepository,
            UserAccountService userAccountService,
            EfiPixClient efiPixClient,
            PaymentMapper paymentMapper,
            AuditLogService auditLogService
    ) {
        this.vipPlanRepository = vipPlanRepository;
        this.paymentRepository = paymentRepository;
        this.provisionRewardRepository = provisionRewardRepository;
        this.userAccountService = userAccountService;
        this.efiPixClient = efiPixClient;
        this.paymentMapper = paymentMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        VipPlanEntity plan = vipPlanRepository.findById(request.planKey())
                .filter(VipPlanEntity::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + request.planKey()));
        UserAccountEntity user = userAccountService.upsertDiscordUser(
                request.discordId(),
                request.minecraftUuid(),
                request.minecraftUsername()
        );
        EfiPixClient.PixChargeResponse pixChargeResponse = efiPixClient.createCharge(plan.getAmount(), request.discordId());
        OffsetDateTime now = OffsetDateTime.now();
        PaymentEntity payment = new PaymentEntity();
        payment.setUserId(user.getId());
        payment.setPlanId(plan.getId());
        payment.setAmount(plan.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTxid(pixChargeResponse.txid());
        payment.setCopyPasteCode(pixChargeResponse.copyPasteCode());
        payment.setQrCodeBase64(pixChargeResponse.qrCodeBase64());
        payment.setDiscordTicketChannelId(request.ticketChannelId());
        payment.setProviderReference(pixChargeResponse.txid());
        payment.setExpiresAt(pixChargeResponse.expiresAt());
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        PaymentEntity savedPayment = paymentRepository.save(payment);
        auditLogService.log("PAYMENT", savedPayment.getId(), "PIX_CREATED", "txid=" + savedPayment.getTxid());
        return paymentMapper.toPaymentResponse(savedPayment, plan);
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        ProvisionRewardEntity reward = provisionRewardRepository.findByPaymentId(payment.getId()).orElse(null);
        return paymentMapper.toPaymentStatusResponse(payment, reward);
    }
}
