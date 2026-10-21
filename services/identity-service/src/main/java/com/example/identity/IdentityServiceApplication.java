package com.example.identity;

import com.example.identity.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class IdentityServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(IdentityServiceApplication.class, args);
  }
}
