package com.eletroflow.backend.repository;

import com.eletroflow.backend.domain.PaymentEntity;
import com.eletroflow.shared.enums.PaymentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

    Optional<PaymentEntity> findByTxid(String txid);

    Optional<PaymentEntity> findTopByUserIdAndPlanIdAndStatusOrderByCreatedAtDesc(String userId, String planId, PaymentStatus status);
}
