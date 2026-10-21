package com.example.identity.controller;

import com.nimbusds.jose.jwk.*;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class JwkController {
  private final RSAKey key;

  @GetMapping("/.well-known/jwks.json")
  @Operation(
      summary = "Public RSA verification key; contains no private material",
      security = {})
  public Map<String, Object> keys() {
    return new JWKSet(key.toPublicJWK()).toJSONObject();
  }
}
