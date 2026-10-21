package com.example.identity.repository;

import com.example.identity.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface UserRepository extends JpaRepository<User, Long> {
  @EntityGraph(attributePaths = "roles")
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @EntityGraph(attributePaths = "roles")
  @Query("select u from User u where u.userId = :id")
  Optional<User> findWithRolesById(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.userId = :id")
  Optional<User> lockById(Long id);
}
