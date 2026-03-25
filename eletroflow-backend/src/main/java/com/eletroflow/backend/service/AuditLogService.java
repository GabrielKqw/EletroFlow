package com.eletroflow.backend.service;

import com.eletroflow.backend.domain.AuditLogEntity;
import com.eletroflow.backend.repository.AuditLogRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String aggregateType, String aggregateId, String action, String details) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAggregateType(aggregateType);
        entity.setAggregateId(aggregateId);
        entity.setAction(action);
        entity.setDetails(details);
        entity.setCreatedAt(OffsetDateTime.now());
        auditLogRepository.save(entity);
    }
}
