package com.example.identity;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.identity.repository.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class IdentityApiTest {
  @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", MYSQL::getJdbcUrl);
    r.add("spring.datasource.username", MYSQL::getUsername);
    r.add("spring.datasource.password", MYSQL::getPassword);
    r.add(
        "identity.jwt.private-key",
        () -> TestKeys.DIRECTORY.resolve("private.pem").toUri().toString());
    r.add(
        "identity.jwt.public-key",
        () -> TestKeys.DIRECTORY.resolve("public.pem").toUri().toString());
    r.add("identity.jwt.issuer", () -> "https://identity.example.test");
    r.add("identity.cors.allowed-origins", () -> "https://shop.example.test");
  }

  @Autowired MockMvc mvc;
  @Autowired JsonMapper json;
  @Autowired UserRepository users;
  @Autowired RefreshTokenRepository tokens;
  @Autowired PasswordEncoder passwords;
  @Autowired JdbcTemplate jdbc;
  static final String PASSWORD = "Correct-Horse-123!";

  @Test
  void registersHashesPasswordAndCreatesProfile() throws Exception {
    String email = email();
    JsonNode data = register(email);
    var user = users.findByEmail(email).orElseThrow();
    assertThat(passwords.matches(PASSWORD, user.getPassword())).isTrue();
    assertThat(user.getPassword()).isNotEqualTo(PASSWORD);
    assertThat(data.path("user").has("password")).isFalse();
    assertThat(data.path("user").path("roles").toString())
        .contains("ROLE_USER")
        .doesNotContain("ROLE_ADMIN");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from profiles where user_id = ?", Long.class, user.getUserId()))
        .isEqualTo(1L);
    String raw = data.path("refreshToken").asString();
    assertThat(tokens.findByUser(user))
        .allSatisfy(t -> assertThat(t.getTokenHash()).hasSize(64).isNotEqualTo(raw));
  }

  @Test
  void duplicateEmailIsConflictAndCaseInsensitive() throws Exception {
    String email = email();
    register(email);
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        Map.of(
                            "fullName",
                            "Other",
                            "email",
                            email.toUpperCase(Locale.ROOT),
                            "password",
                            PASSWORD))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.httpStatus").value("CONFLICT"));
  }

  @Test
  void successfulLoginAndCurrentUser() throws Exception {
    String email = email();
    register(email);
    JsonNode data = login(email);
    mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(data)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value(email));
  }

  @Test
  void invalidCredentialsAreUnauthorized() throws Exception {
    String email = email();
    register(email);
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("email", email, "password", "incorrect-password"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.httpStatus").value("UNAUTHORIZED"));
  }

  @Test
  void unauthenticatedRequestsAreRejected() throws Exception {
    mvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").exists());
    mvc.perform(
            post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"invalid\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer malformed"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void usersCannotListOthersOrChangeStatusOrValidateTokens() throws Exception {
    JsonNode first = register(email()), second = register(email());
    String auth = bearer(first);
    long other = second.path("user").path("userId").asLong();
    mvc.perform(get("/api/v1/users").header("Authorization", auth))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/users/" + other).header("Authorization", auth))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/v1/users/" + other + "/status")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"LOCKED\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/auth/validate")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("token", first.path("accessToken").asString()))))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/users/" + first.path("user").path("userId").asLong())
                .header("Authorization", auth))
        .andExpect(status().isOk());
  }

  @Test
  void adminCanListValidateAndUpdateStatus() throws Exception {
    String adminEmail = email();
    JsonNode admin = register(adminEmail);
    promote(admin);
    admin = login(adminEmail);
    mvc.perform(get("/api/v1/users").header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.users").isArray());
    mvc.perform(
            post("/api/v1/auth/validate")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("token", admin.path("accessToken").asString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.valid").value(true));
    JsonNode target = register(email());
    mvc.perform(
            patch("/api/v1/users/" + target.path("user").path("userId").asLong() + "/status")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accountStatus").value("INACTIVE"));
    mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(target)))
        .andExpect(status().isUnauthorized());
    refresh(target.path("refreshToken").asString()).andExpect(status().isUnauthorized());
  }

  @Test
  void refreshRotatesAndReplayRevokesSuccessor() throws Exception {
    JsonNode initial = register(email());
    String old = initial.path("refreshToken").asString();
    JsonNode rotated = data(refresh(old).andExpect(status().isOk()).andReturn());
    String next = rotated.path("refreshToken").asString();
    assertThat(next).isNotEqualTo(old);
    refresh(old).andExpect(status().isUnauthorized());
    refresh(next).andExpect(status().isUnauthorized());
  }

  @Test
  void concurrentRefreshDoesNotCreateTwoLiveSuccessors() throws Exception {
    String old = register(email()).path("refreshToken").asString();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      CountDownLatch start = new CountDownLatch(1);
      Callable<MvcResult> action =
          () -> {
            start.await();
            return refresh(old).andReturn();
          };
      Future<MvcResult> a = executor.submit(action), b = executor.submit(action);
      start.countDown();
      List<MvcResult> results = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
      assertThat(results.stream().map(v -> v.getResponse().getStatus()))
          .containsExactlyInAnyOrder(200, 401);
      MvcResult success =
          results.stream()
              .filter(v -> v.getResponse().getStatus() == 200)
              .findFirst()
              .orElseThrow();
      refresh(data(success).path("refreshToken").asString()).andExpect(status().isUnauthorized());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void logoutRevokesRefreshButExistingAccessTokenRemainsValid() throws Exception {
    JsonNode initial = register(email());
    String raw = initial.path("refreshToken").asString();
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/v1/auth/logout")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body(Map.of("refreshToken", raw))))
          .andExpect(status().isOk());
    refresh(raw).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(initial)))
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(strings = {"INACTIVE", "LOCKED"})
  void unavailableAccountsCannotLoginRefreshOrUseAccessToken(String state) throws Exception {
    String email = email();
    JsonNode initial = register(email);
    jdbc.update("update users set status = ? where email = ?", state, email);
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("email", email, "password", PASSWORD))))
        .andExpect(status().isUnauthorized());
    refresh(initial.path("refreshToken").asString()).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(initial)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expiredOrUnknownRefreshTokensFail() throws Exception {
    JsonNode initial = register(email());
    jdbc.update(
        "update refresh_tokens set expires_at = '2020-01-01' where user_id = ?",
        initial.path("user").path("userId").asLong());
    refresh(initial.path("refreshToken").asString()).andExpect(status().isUnauthorized());
    refresh("x".repeat(43)).andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsInvalidInputAndPrivilegeInjection() throws Exception {
    mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.data.email").exists());
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        Map.of(
                            "email",
                            email(),
                            "fullName",
                            "User",
                            "password",
                            PASSWORD,
                            "roles",
                            List.of("ROLE_ADMIN")))))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(Map.of("email", email(), "fullName", "User", "password", "界".repeat(30)))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void swaggerAndPublicKeysAreAccessibleWithoutPrivateKeyMaterial() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    mvc.perform(get("/.well-known/jwks.json"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
        .andExpect(jsonPath("$.keys[0].d").doesNotExist());
  }

  @Test
  void corsAllowsOnlyConfiguredOrigins() throws Exception {
    mvc.perform(
            options("/api/v1/auth/login")
                .header("Origin", "https://shop.example.test")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "https://shop.example.test"));
    mvc.perform(
            options("/api/v1/auth/login")
                .header("Origin", "https://evil.example.test")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidPaginationAndMissingUsersReturnCorrectErrors() throws Exception {
    JsonNode admin = register(email());
    promote(admin);
    mvc.perform(get("/api/v1/users").param("size", "101").header("Authorization", bearer(admin)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.httpStatus").value("BAD_REQUEST"));
    mvc.perform(get("/api/v1/users/999999999").header("Authorization", bearer(admin)))
        .andExpect(status().isNotFound());
  }

  private JsonNode register(String email) throws Exception {
    return data(
        mvc.perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        body(
                            Map.of(
                                "fullName",
                                "Test Customer",
                                "email",
                                email,
                                "password",
                                PASSWORD))))
            .andExpect(status().isCreated())
            .andReturn());
  }

  private JsonNode login(String email) throws Exception {
    return data(
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of("email", email, "password", PASSWORD))))
            .andExpect(status().isOk())
            .andReturn());
  }

  private ResultActions refresh(String token) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(Map.of("refreshToken", token))));
  }

  private String body(Object value) {
    return json.writeValueAsString(value);
  }

  private JsonNode data(MvcResult result) throws Exception {
    return json.readTree(result.getResponse().getContentAsString()).path("data");
  }

  private String bearer(JsonNode data) {
    return "Bearer " + data.path("accessToken").asString();
  }

  private String email() {
    return UUID.randomUUID() + "@example.test";
  }

  private void promote(JsonNode data) {
    jdbc.update(
        "insert into user_roles (user_id, role_id) select ?, id from roles where name ="
            + " 'ROLE_ADMIN'",
        data.path("user").path("userId").asLong());
  }
}
