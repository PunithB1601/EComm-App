package com.example.identity.service.impl;

import com.example.identity.config.JwtProperties;
import com.example.identity.entity.User;
import com.example.identity.service.JwtService;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final JwtProperties properties;
  private final Clock clock;

  @Override
  public String generateAccessToken(User user) {
    Instant now = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.getUserId().toString())
            .issuer(properties.getIssuer())
            .audience(List.of(properties.getAudience()))
            .issuedAt(now)
            .expiresAt(now.plus(properties.getAccessTtl()))
            .id(UUID.randomUUID().toString())
            .claim("email", user.getEmail())
            .claim("token_use", "access")
            .claim(
                "roles",
                user.getRoles().stream().map(r -> r.getRoleName().name()).sorted().toList())
            .build();
    JwsHeader headers =
        JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.getKeyId()).build();
    return encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
  }

  @Override
  public Jwt validateAccessToken(String token) {
    return decoder.decode(token);
  }

  @Override
  public String extractUsername(String token) {
    return validateAccessToken(token).getClaimAsString("email");
  }

  @Override
  public List<String> extractRoles(String token) {
    return validateAccessToken(token).getClaimAsStringList("roles");
  }

  @Override
  public Instant extractExpiration(String token) {
    return validateAccessToken(token).getExpiresAt();
  }
}
