package com.example.identity.security;

import com.example.identity.service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final CustomUserDetailsService users;
  private final CustomAuthenticationEntryPoint entryPoint;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
      try {
        var claims = jwt.validateAccessToken(header.substring(7));
        var principal = users.loadById(Long.valueOf(claims.getSubject()));
        new AccountStatusUserDetailsChecker().check(principal);
        var auth =
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (JwtException | AuthenticationException | IllegalArgumentException e) {
        SecurityContextHolder.clearContext();
        entryPoint.commence(request, response, new BadCredentialsException("Invalid token"));
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
