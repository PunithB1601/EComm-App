package com.ecomm.identity.domain;

import jakarta.persistence.*;

@Entity
@Table(name="refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false)
    private User user;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    @Column(name="token_hash", nullable=false, unique=true, length=64)
    private String tokenHash;

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    @Column(name="expires_at", nullable=false)
    private java.time.Instant expiresAt;

    public java.time.Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(java.time.Instant expiresAt) { this.expiresAt = expiresAt; }
    @Column(name="revoked_at")
    private java.time.Instant revokedAt;

    public java.time.Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(java.time.Instant revokedAt) { this.revokedAt = revokedAt; }
    @Column(name="created_at", nullable=false, updatable=false)
    private java.time.Instant createdAt;

    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    @PrePersist
    void onCreate() {
        var now = java.time.Instant.now();
        createdAt = now;
    }
}
