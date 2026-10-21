package com.example.identity.entity;

import com.example.identity.enums.AccountStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long userId;

  @NotBlank
  @Size(max = 200)
  @Column(name = "full_name", nullable = false, length = 200)
  private String fullName;

  @NotBlank
  @Email
  @Size(max = 254)
  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @JsonIgnore
  @NotBlank
  @Column(name = "password_hash", nullable = false)
  private String password;

  @NotNull
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "status", nullable = false, length = 20)
  private AccountStatus accountStatus = AccountStatus.ACTIVE;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = updatedAt = LocalDateTime.now(ZoneOffset.UTC);
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now(ZoneOffset.UTC);
  }
}
