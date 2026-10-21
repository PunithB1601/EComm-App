package com.example.identity.repository;

import com.example.identity.entity.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  @Query("select t.user.userId from RefreshToken t where t.tokenHash = :hash")
  Optional<Long> findOwnerIdByHash(String hash);

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  List<RefreshToken> findByUser(User user);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from RefreshToken t where t.tokenHash = :hash")
  Optional<RefreshToken> lockByHash(String hash);

  @Modifying
  @Query("update RefreshToken t set t.revoked = true where t.familyId = :family")
  int revokeFamily(String family);

  @Modifying
  @Query("update RefreshToken t set t.revoked = true where t.user.userId = :userId")
  int revokeAllForUser(Long userId);

  // Retain expired records for a grace window so replay evidence is not immediately lost.
  @Modifying
  @Query("delete from RefreshToken t where t.expiryDate < :cutoff")
  int deleteExpiredBefore(Instant cutoff);

  List<RefreshToken> findByRevokedTrueAndExpiryDateBefore(Instant cutoff);
}
