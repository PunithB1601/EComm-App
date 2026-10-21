package com.example.identity.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "identity.jwt")
@Validated
@Getter
@Setter
public class JwtProperties {
  @NotBlank private String issuer;
  @NotBlank private String audience;
  @NotBlank private String keyId;
  @NotNull private Resource privateKey;
  @NotNull private Resource publicKey;
  @NotNull private Duration accessTtl = Duration.ofMinutes(15);
  @NotNull private Duration refreshTtl = Duration.ofDays(7);

  @AssertTrue(message = "JWT lifetimes must be positive, with access TTL at most one hour")
  public boolean isLifetimeValid() {
    return accessTtl != null
        && refreshTtl != null
        && !accessTtl.isNegative()
        && !accessTtl.isZero()
        && accessTtl.compareTo(Duration.ofHours(1)) <= 0
        && refreshTtl.compareTo(accessTtl) > 0;
  }
}
