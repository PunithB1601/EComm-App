package com.example.identity.service;

import com.example.identity.entity.User;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

public interface JwtService {
  String generateAccessToken(User user);

  Jwt validateAccessToken(String token);

  String extractUsername(String token);

  List<String> extractRoles(String token);

  Instant extractExpiration(String token);
}
