package com.example.identity.config;

import com.example.identity.security.*;
import com.example.identity.service.JwtService;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  AuthenticationManager authenticationManager(
      CustomUserDetailsService users, PasswordEncoder encoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtService jwt,
      CustomUserDetailsService users,
      CustomAuthenticationEntryPoint entry,
      CustomAccessDeniedHandler denied)
      throws Exception {
    // Tokens are sent explicitly in Authorization/body, never automatically in authentication
    // cookies.
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(e -> e.authenticationEntryPoint(entry).accessDeniedHandler(denied))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/.well-known/jwks.json")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/validate")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/v1/users/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .addFilterBefore(
            new JwtAuthenticationFilter(jwt, users, entry),
            UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${identity.cors.allowed-origins:}") String origins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(
        Arrays.stream(origins.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(false);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
