package com.example.identity;

import static org.assertj.core.api.Assertions.*;

import com.example.identity.config.*;
import com.example.identity.entity.*;
import com.example.identity.enums.RoleName;
import com.example.identity.service.impl.JwtServiceImpl;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.*;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.oauth2.jwt.*;

class JwtServiceTest {
  JwtServiceImpl service;
  RSAKey key;
  JwtProperties properties;
  Clock clock = Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setup() throws Exception {
    properties = new JwtProperties();
    properties.setIssuer("https://identity.example.test");
    properties.setAudience("ecomm-api");
    properties.setKeyId("test-key");
    properties.setPrivateKey(new FileSystemResource(TestKeys.DIRECTORY.resolve("private.pem")));
    properties.setPublicKey(new FileSystemResource(TestKeys.DIRECTORY.resolve("public.pem")));
    JwtKeyConfig config = new JwtKeyConfig();
    key = config.signingKey(properties);
    service =
        new JwtServiceImpl(
            config.jwtEncoder(key), config.jwtDecoder(key, properties, clock), properties, clock);
  }

  @Test
  void generatesAndExtractsSignedClaims() {
    User user = new User();
    user.setUserId(42L);
    user.setEmail("user@example.test");
    Role role = new Role();
    role.setRoleName(RoleName.ROLE_USER);
    user.getRoles().add(role);
    String token = service.generateAccessToken(user);
    assertThat(service.validateAccessToken(token).getSubject()).isEqualTo("42");
    assertThat(service.extractUsername(token)).isEqualTo("user@example.test");
    assertThat(service.extractRoles(token)).containsExactly("ROLE_USER");
    assertThat(service.extractExpiration(token)).isEqualTo(clock.instant().plusSeconds(900));
    assertThat(service.validateAccessToken(token).getId()).isNotBlank();
  }

  @Test
  void rejectsExpiredToken() throws Exception {
    assertThatThrownBy(
            () ->
                service.validateAccessToken(
                    signed(
                        key.toRSAPrivateKey(),
                        properties.getIssuer(),
                        properties.getAudience(),
                        clock.instant().minusSeconds(60),
                        "RS256")))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsInvalidSignature() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    String token =
        signed(
            (RSAPrivateKey) generator.generateKeyPair().getPrivate(),
            properties.getIssuer(),
            properties.getAudience(),
            clock.instant().plusSeconds(900),
            "RS256");
    assertThatThrownBy(() -> service.validateAccessToken(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsWrongIssuer() throws Exception {
    String token =
        signed(
            key.toRSAPrivateKey(),
            "https://attacker.example.test",
            properties.getAudience(),
            clock.instant().plusSeconds(900),
            "RS256");
    assertThatThrownBy(() -> service.validateAccessToken(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsWrongAudience() throws Exception {
    String token =
        signed(
            key.toRSAPrivateKey(),
            properties.getIssuer(),
            "different-api",
            clock.instant().plusSeconds(900),
            "RS256");
    assertThatThrownBy(() -> service.validateAccessToken(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsUnsupportedAlgorithm() throws Exception {
    String token =
        signed(
            key.toRSAPrivateKey(),
            properties.getIssuer(),
            properties.getAudience(),
            clock.instant().plusSeconds(900),
            "RS512");
    assertThatThrownBy(() -> service.validateAccessToken(token)).isInstanceOf(JwtException.class);
  }

  private String signed(
      RSAPrivateKey privateKey, String issuer, String audience, Instant expiry, String algorithm)
      throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject("42")
            .issuer(issuer)
            .audience(audience)
            .issueTime(Date.from(clock.instant().minusSeconds(120)))
            .expirationTime(Date.from(expiry))
            .jwtID(UUID.randomUUID().toString())
            .claim("email", "user@example.test")
            .claim("token_use", "access")
            .claim("roles", List.of("ROLE_USER"))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.parse(algorithm)), claims);
    jwt.sign(new RSASSASigner(privateKey));
    return jwt.serialize();
  }
}
