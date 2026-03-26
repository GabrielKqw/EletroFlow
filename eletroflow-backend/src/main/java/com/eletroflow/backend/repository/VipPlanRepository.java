package com.eletroflow.backend.repository;

import com.eletroflow.backend.domain.VipPlanEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VipPlanRepository extends JpaRepository<VipPlanEntity, String> {

    List<VipPlanEntity> findByActiveTrueOrderByAmountAsc();
}
