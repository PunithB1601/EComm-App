package com.example.identity.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "profiles")
public class Profile {
  @Id
  @Column(name = "user_id")
  private Long userId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  @Column(name = "last_name", length = 100)
  private String lastName;

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  @Column(length = 32)
  private String phone;

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  @Column(name = "date_of_birth")
  private java.time.LocalDate dateOfBirth;

  public java.time.LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(java.time.LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  @Column(name = "updated_at", nullable = false)
  private java.time.Instant updatedAt;

  public java.time.Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(java.time.Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @PrePersist
  void onCreate() {
    var now = java.time.Instant.now();
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = java.time.Instant.now();
  }
}
