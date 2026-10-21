package com.example.identity.repository;

import com.example.identity.entity.Role;
import com.example.identity.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByRoleName(RoleName roleName);
}
