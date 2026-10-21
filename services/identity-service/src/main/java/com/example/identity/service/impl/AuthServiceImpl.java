package com.example.identity.service.impl;

import com.example.identity.config.JwtProperties;
import com.example.identity.dto.request.*;
import com.example.identity.dto.response.*;
import com.example.identity.entity.*;
import com.example.identity.enums.*;
import com.example.identity.exception.*;
import com.example.identity.mapper.UserMapper;
import com.example.identity.repository.*;
import com.example.identity.security.IdentityPrincipal;
import com.example.identity.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final UserRepository users;
  private final RoleRepository roles;
  private final RefreshTokenRepository tokens;
  private final PasswordEncoder passwords;
  private final AuthenticationManager authentication;
  private final JwtService jwt;
  private final JwtProperties properties;
  private final UserMapper mapper;
  private final Clock clock;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    validatePasswordBytes(request.password());
    String email = normalize(request.email());
    if (users.existsByEmail(email))
      throw new ApplicationException(HttpStatus.CONFLICT, "Email already registered");
    User user = new User();
    user.setEmail(email);
    user.setFullName(request.fullName().strip());
    user.setPassword(passwords.encode(request.password()));
    user.getRoles()
        .add(
            roles
                .findByRoleName(RoleName.ROLE_USER)
                .orElseThrow(
                    () -> new IllegalStateException("Required role migration is missing")));
    users.saveAndFlush(user);
    Profile profile = new Profile();
    profile.setUser(user);
    String[] names = user.getFullName().split("\\s+", 2);
    profile.setFirstName(names[0].substring(0, Math.min(100, names[0].length())));
    if (names.length > 1)
      profile.setLastName(names[1].substring(0, Math.min(100, names[1].length())));
    entityManager.persist(profile);
    return issue(user, UUID.randomUUID().toString());
  }

  @Override
  @Transactional
  public AuthResponse login(LoginRequest request) {
    validatePasswordBytes(request.password());
    var result =
        authentication.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                normalize(request.email()), request.password()));
    Long id = ((IdentityPrincipal) result.getPrincipal()).userId();
    User user =
        users.lockById(id).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    // A concurrent account-status change must win over an earlier credential lookup.
    entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
    ensureActive(user);
    return issue(user, UUID.randomUUID().toString());
  }

  @Override
  @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
  public AuthResponse refreshToken(RefreshTokenRequest request) {
    RefreshToken old = lockToken(request.refreshToken());
    if (old.isRevoked() || !old.getExpiryDate().isAfter(clock.instant())) {
      tokens.revokeFamily(old.getFamilyId());
      throw new InvalidRefreshTokenException();
    }
    if (old.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
      tokens.revokeAllForUser(old.getUser().getUserId());
      throw new InvalidRefreshTokenException();
    }
    old.setRevoked(true);
    return issue(old.getUser(), old.getFamilyId());
  }

  @Override
  @Transactional
  public void logout(RefreshTokenRequest request) {
    String hash = hash(request.refreshToken());
    Optional<Long> owner = tokens.findOwnerIdByHash(hash);
    if (owner.isEmpty()) return; // Idempotent; do not reveal whether a token exists.
    users.lockById(owner.get()).orElseThrow(InvalidRefreshTokenException::new);
    tokens.lockByHash(hash).ifPresent(t -> tokens.revokeFamily(t.getFamilyId()));
  }

  @Override
  @Transactional(readOnly = true)
  public TokenValidationResponse validateToken(String token) {
    var claims = jwt.validateAccessToken(token);
    User user =
        users
            .findWithRolesById(Long.valueOf(claims.getSubject()))
            .orElseThrow(() -> new BadCredentialsException("Invalid account"));
    ensureActive(user);
    return new TokenValidationResponse(
        true, claims.getSubject(), claims.getClaimAsStringList("roles"), claims.getExpiresAt());
  }

  private RefreshToken lockToken(String raw) {
    String hash = hash(raw);
    Long owner = tokens.findOwnerIdByHash(hash).orElseThrow(InvalidRefreshTokenException::new);
    // All session mutations lock the account first, then the token: consistent locking order.
    users.lockById(owner).orElseThrow(InvalidRefreshTokenException::new);
    return tokens.lockByHash(hash).orElseThrow(InvalidRefreshTokenException::new);
  }

  private AuthResponse issue(User user, String family) {
    byte[] random = new byte[32];
    RANDOM.nextBytes(random);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    RefreshToken token = new RefreshToken();
    token.setUser(user);
    token.setTokenHash(hash(raw));
    token.setFamilyId(family);
    token.setExpiryDate(clock.instant().plus(properties.getRefreshTtl()));
    tokens.save(token);
    return new AuthResponse(
        jwt.generateAccessToken(user),
        raw,
        "Bearer",
        properties.getAccessTtl().toSeconds(),
        mapper.toResponse(user));
  }

  private static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private static String normalize(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  private static void validatePasswordBytes(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ApplicationException(
          HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
  }

  private static void ensureActive(User user) {
    if (user.getAccountStatus() != AccountStatus.ACTIVE)
      throw new DisabledException("Account unavailable");
  }
}
