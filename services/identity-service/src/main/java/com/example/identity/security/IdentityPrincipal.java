package com.example.identity.security;

import com.example.identity.entity.User;
import com.example.identity.enums.AccountStatus;
import java.util.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record IdentityPrincipal(
    Long userId,
    String email,
    String passwordHash,
    AccountStatus status,
    List<GrantedAuthority> authorities)
    implements UserDetails {
  public static IdentityPrincipal from(User user) {
    return new IdentityPrincipal(
        user.getUserId(),
        user.getEmail(),
        user.getPassword(),
        user.getAccountStatus(),
        user.getRoles().stream()
            .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r.getRoleName().name()))
            .toList());
  }

  public Long getUserId() {
    return userId;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return status != AccountStatus.LOCKED;
  }

  @Override
  public boolean isEnabled() {
    return status == AccountStatus.ACTIVE;
  }

  @Override
  public String toString() {
    return "IdentityPrincipal[userId=" + userId + "]";
  }
}
