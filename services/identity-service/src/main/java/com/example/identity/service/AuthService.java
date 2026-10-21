package com.example.identity.service;

import com.example.identity.dto.request.*;
import com.example.identity.dto.response.*;

public interface AuthService {
  AuthResponse register(RegisterRequest request);

  AuthResponse login(LoginRequest request);

  AuthResponse refreshToken(RefreshTokenRequest request);

  void logout(RefreshTokenRequest request);

  TokenValidationResponse validateToken(String token);
}
