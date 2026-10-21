package com.example.identity.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI identityOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Identity Service API")
                .version("v1")
                .description(
                    "Registration, RSA bearer authentication and rotating refresh sessions. Never"
                        + " paste production credentials into shared Swagger sessions."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }

  @Bean
  OpenApiCustomizer errorResponses() {
    return api ->
        api.getPaths()
            .values()
            .forEach(
                path ->
                    path.readOperations()
                        .forEach(
                            operation -> {
                              for (String code :
                                  new String[] {"400", "401", "403", "404", "409", "500"}) {
                                operation
                                    .getResponses()
                                    .addApiResponse(
                                        code,
                                        new ApiResponse()
                                            .description(
                                                switch (code) {
                                                  case "400" ->
                                                      "Invalid request or bean validation error";
                                                  case "401" ->
                                                      "Invalid credentials, token or unavailable"
                                                          + " account";
                                                  case "403" -> "Insufficient permissions";
                                                  case "404" -> "User not found";
                                                  case "409" ->
                                                      "Duplicate email or conflicting record";
                                                  default -> "Unexpected server error";
                                                })
                                            .content(
                                                new Content()
                                                    .addMediaType(
                                                        "application/json",
                                                        new MediaType()
                                                            .schema(
                                                                new ObjectSchema()
                                                                    .addProperty(
                                                                        "message",
                                                                        new StringSchema())
                                                                    .addProperty(
                                                                        "data", new ObjectSchema())
                                                                    .addProperty(
                                                                        "httpStatus",
                                                                        new StringSchema())))));
                              }
                            }));
  }
}
