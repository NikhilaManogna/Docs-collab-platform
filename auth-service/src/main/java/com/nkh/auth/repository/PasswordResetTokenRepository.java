package com.nkh.auth.repository;

import com.nkh.auth.domain.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByToken(String token);

    List<PasswordResetTokenEntity> findAllByUserIdAndUsedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);
}
