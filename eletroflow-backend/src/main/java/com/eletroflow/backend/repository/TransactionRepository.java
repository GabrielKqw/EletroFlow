package com.eletroflow.backend.repository;

import com.eletroflow.backend.domain.TransactionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    Optional<TransactionEntity> findByProviderEventId(String providerEventId);
}
