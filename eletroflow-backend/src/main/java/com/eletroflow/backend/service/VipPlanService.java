package com.eletroflow.backend.service;

import com.eletroflow.backend.repository.VipPlanRepository;
import com.eletroflow.shared.dto.VipPlanResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VipPlanService {

    private final VipPlanRepository vipPlanRepository;

    public VipPlanService(VipPlanRepository vipPlanRepository) {
        this.vipPlanRepository = vipPlanRepository;
    }

    @Transactional(readOnly = true)
    public List<VipPlanResponse> listActivePlans() {
        return vipPlanRepository.findByActiveTrueOrderByAmountAsc().stream()
                .map(plan -> new VipPlanResponse(
                        plan.getId(),
                        plan.getDisplayName(),
                        plan.getAmount(),
                        plan.getCurrency(),
                        plan.getDiscordRoleId(),
                        plan.getDurationDays()
                ))
                .toList();
    }
}
