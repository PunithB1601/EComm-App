package com.example.identity.security;

import com.example.identity.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository users;

  @Override
  @Transactional(readOnly = true)
  public IdentityPrincipal loadUserByUsername(String email) {
    return users
        .findByEmail(email.strip().toLowerCase(Locale.ROOT))
        .map(IdentityPrincipal::from)
        .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }

  @Transactional(readOnly = true)
  public IdentityPrincipal loadById(Long id) {
    return users
        .findWithRolesById(id)
        .map(IdentityPrincipal::from)
        .orElseThrow(() -> new UsernameNotFoundException("Invalid account"));
  }
}
