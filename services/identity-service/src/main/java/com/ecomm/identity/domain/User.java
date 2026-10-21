package com.ecomm.identity.domain;

import jakarta.persistence.*;

@Entity
@Table(name="users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
    @Column(nullable=false, unique=true, length=254)
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    @Column(name="password_hash", nullable=false)
    private String passwordHash;

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private UserStatus status = UserStatus.ACTIVE;

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    @Column(name="created_at", nullable=false, updatable=false)
    private java.time.Instant createdAt;

    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    @Column(name="updated_at", nullable=false)
    private java.time.Instant updatedAt;

    public java.time.Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.Instant updatedAt) { this.updatedAt = updatedAt; }
    @PrePersist
    void onCreate() {
        var now = java.time.Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate
    void onUpdate() { updatedAt = java.time.Instant.now(); }
}
