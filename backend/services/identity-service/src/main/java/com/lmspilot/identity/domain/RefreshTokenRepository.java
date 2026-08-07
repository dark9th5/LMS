package com.lmspilot.identity.domain;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity,UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshTokenEntity t where t.tokenHash=:hash")
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("hash") String hash);

    List<RefreshTokenEntity> findAllByUserIdOrderByIssuedAtDesc(UUID userId);
    long deleteByExpiresAtBefore(Instant cutoff);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt=:now,t.revokedReason=:reason where t.userId=:userId and t.revokedAt is null")
    int revokeAllByUserId(@Param("userId") UUID userId,@Param("now") Instant now,@Param("reason") String reason);
}
