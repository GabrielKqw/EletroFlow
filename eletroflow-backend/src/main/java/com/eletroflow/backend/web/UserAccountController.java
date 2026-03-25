package com.eletroflow.backend.web;

import com.eletroflow.backend.service.UserAccountService;
import com.eletroflow.shared.dto.LinkMinecraftAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void linkMinecraftAccount(@Valid @RequestBody LinkMinecraftAccountRequest request) {
        userAccountService.linkAccount(request);
    }
}
