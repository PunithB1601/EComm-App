package com.example.identity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Pattern(regexp = "[a-f0-9]{64}")
  @JsonIgnore
  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @NotNull
  @Column(name = "expires_at", nullable = false)
  private Instant expiryDate;

  @Column(nullable = false)
  private boolean revoked;

  @NotBlank
  @Size(max = 36)
  @Column(name = "family_id", nullable = false, length = 36)
  private String familyId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }
}
