package com.example.identity.security;

import com.example.identity.dto.response.ApiResponse;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
  private final JsonMapper mapper;

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
      throws IOException {
    response.setStatus(401);
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(
        response.getOutputStream(),
        ApiResponse.of(
            "Authentication required or invalid access token", null, HttpStatus.UNAUTHORIZED));
  }
}
