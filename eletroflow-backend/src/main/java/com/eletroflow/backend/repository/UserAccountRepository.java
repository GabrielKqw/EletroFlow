package com.eletroflow.backend.repository;

import com.eletroflow.backend.domain.UserAccountEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, String> {

    Optional<UserAccountEntity> findByDiscordId(String discordId);
}
