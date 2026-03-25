package com.eletroflow.backend.repository;

import com.eletroflow.backend.domain.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {
}
