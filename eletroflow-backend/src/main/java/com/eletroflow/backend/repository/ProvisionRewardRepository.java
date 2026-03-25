package com.eletroflow.backend.repository;

import com.eletroflow.backend.domain.ProvisionRewardEntity;
import com.eletroflow.shared.enums.ProvisionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvisionRewardRepository extends JpaRepository<ProvisionRewardEntity, String> {

    List<ProvisionRewardEntity> findTop20ByStatusOrderByCreatedAtAsc(ProvisionStatus status);

    Optional<ProvisionRewardEntity> findByPaymentId(String paymentId);
}
