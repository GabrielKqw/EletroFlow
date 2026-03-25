package com.eletroflow.backend.web;

import com.eletroflow.backend.service.RewardDispatchService;
import com.eletroflow.shared.dto.PendingRewardResponse;
import com.eletroflow.shared.dto.RewardAckRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    private final RewardDispatchService rewardDispatchService;

    public RewardController(RewardDispatchService rewardDispatchService) {
        this.rewardDispatchService = rewardDispatchService;
    }

    @GetMapping("/pending")
    public List<PendingRewardResponse> pendingRewards() {
        return rewardDispatchService.claimPendingRewards();
    }

    @PostMapping("/{rewardId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markCompleted(@PathVariable String rewardId, @Valid @RequestBody RewardAckRequest request) {
        rewardDispatchService.markCompleted(rewardId, request);
    }

    @PostMapping("/{rewardId}/fail")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markFailed(@PathVariable String rewardId, @Valid @RequestBody RewardAckRequest request) {
        rewardDispatchService.markFailed(rewardId, request);
    }
}
