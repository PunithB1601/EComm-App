package com.example.identity.config;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.time.Clock;
import java.util.Base64;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class JwtKeyConfig {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public RSAKey signingKey(JwtProperties properties) throws Exception {
    KeyFactory factory = KeyFactory.getInstance("RSA");
    RSAPrivateKey privateKey;
    RSAPublicKey publicKey;
    try (var in = properties.getPrivateKey().getInputStream()) {
      privateKey =
          (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(pem(in.readAllBytes())));
    }
    try (var in = properties.getPublicKey().getInputStream()) {
      publicKey =
          (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(pem(in.readAllBytes())));
    }
    if (publicKey.getModulus().bitLength() < 2048
        || !publicKey.getModulus().equals(privateKey.getModulus())) {
      throw new IllegalStateException("JWT requires matching RSA keys of at least 2048 bits");
    }
    return new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(properties.getKeyId())
        .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
        .keyUse(KeyUse.SIGNATURE)
        .build();
  }

  private byte[] pem(byte[] bytes) {
    String text =
        new String(bytes, StandardCharsets.US_ASCII)
            .replaceAll("-----[^-]+-----", "")
            .replaceAll("\\s", "");
    return Base64.getDecoder().decode(text);
  }

  @Bean
  public JwtEncoder jwtEncoder(RSAKey key) {
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
  }

  @Bean
  public JwtDecoder jwtDecoder(RSAKey key, JwtProperties properties, Clock clock) throws Exception {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey())
            .signatureAlgorithm(SignatureAlgorithm.RS256)
            .build();
    JwtTimestampValidator time = new JwtTimestampValidator(java.time.Duration.ofSeconds(30));
    time.setClock(clock);
    OAuth2TokenValidator<Jwt> claims =
        jwt -> {
          boolean valid =
              jwt.getSubject() != null
                  && jwt.getSubject().matches("[1-9][0-9]{0,17}")
                  && jwt.getExpiresAt() != null
                  && jwt.getIssuedAt() != null
                  && !jwt.getIssuedAt().isAfter(clock.instant().plusSeconds(30))
                  && jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                  && jwt.getAudience().contains(properties.getAudience())
                  && "access".equals(jwt.getClaimAsString("token_use"))
                  && jwt.getId() != null
                  && jwt.getClaimAsString("email") != null
                  && jwt.getClaimAsStringList("roles") != null;
          return valid
              ? OAuth2TokenValidatorResult.success()
              : OAuth2TokenValidatorResult.failure(
                  new OAuth2Error("invalid_token", "Required token claims are invalid", null));
        };
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            time, new JwtIssuerValidator(properties.getIssuer()), claims));
    return decoder;
  }
}
