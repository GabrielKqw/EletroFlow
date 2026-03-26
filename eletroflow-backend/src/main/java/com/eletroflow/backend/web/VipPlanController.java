package com.eletroflow.backend.web;

import com.eletroflow.backend.service.VipPlanService;
import com.eletroflow.shared.dto.VipPlanResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vip-plans")
public class VipPlanController {

    private final VipPlanService vipPlanService;

    public VipPlanController(VipPlanService vipPlanService) {
        this.vipPlanService = vipPlanService;
    }

    @GetMapping
    public List<VipPlanResponse> listPlans() {
        return vipPlanService.listActivePlans();
    }
}
