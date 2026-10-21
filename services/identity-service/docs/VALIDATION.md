# Validation evidence

Validated in the cloud workspace on 2026-10-21. These results establish the current implementation and local development workflow; they do not establish a production deployment.

## Maven build and tests

`./mvnw clean verify` completed successfully with Java 17 release compilation. The host used a full JDK 21 with `--release 17`; the multi-stage Docker build also compiled successfully using a full JDK 17.

| Suite | Executed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| IdentityApiTest | 17 | 0 | 0 | 0 |
| JwtServiceTest | 6 | 0 | 0 | 0 |
| MigrationUpgradeTest | 1 | 0 | 0 | 0 |
| UserServiceTest | 2 | 0 | 0 | 0 |
| Total | 26 | 0 | 0 | 0 |

The API tests use real MySQL 8.4 through Testcontainers and the full Spring Security/MockMvc stack. Coverage includes registration, duplicate email, BCrypt verification, login, unauthorized and forbidden requests, owner/admin permissions, token rotation, replay and concurrent refresh, logout semantics, inactive/locked accounts, invalid input, pagination validation, CORS, Swagger and public-key output.

JWT unit tests verify valid claims, expiration, wrong signature, issuer, audience and unsupported signing algorithm. Mockito tests cover account revocation behavior and missing-user errors. The migration test starts with populated V1 tables, upgrades to V2, verifies retained account/profile/address/role data and invalidated old sessions, and confirms rerunning migrations is a no-op. V1 remains unchanged.

An attempted JUnit 5 override failed before the Spring integration tests could start because SpringExtension requires a JUnit 6 method. The tested configuration uses Boot-managed JUnit 6.0.3 instead. No tests are disabled to hide this incompatibility.

## Docker and functional requests

- Multi-stage Dockerfile built successfully using Maven/JDK 17 and a Java 17 JRE runtime.
- Docker Compose started MySQL and Identity Service with the prod profile and mounted development key files.
- Runtime Java version was verified as 17.0.20.1.
- MySQL health check completed; Flyway migrations and Hibernate validation completed.
- `python3 scripts/smoke-test.py http://localhost:8081` passed against the running Docker service.
- The packaged JAR also started independently outside Docker on a separate local port with the dev profile and MySQL connection.
- `python3 scripts/smoke-test.py http://localhost:8082` passed against that standalone process.

The smoke checks make real HTTP requests for registration, login, current user, insufficient-role rejection, missing authentication, refresh rotation/replay, successor revocation, logout, OpenAPI and public verification keys. Tokens/passwords are not printed by the script. It creates a fresh test account per run and should be used only against development instances.

The cloud machine requires a writable Maven/Docker cache and its configured outbound proxy. Those machine-specific settings were kept outside the repository. Docker builds used optional secret mounts for Maven settings and the platform's trusted Java certificate store; signature, checksum and TLS verification remained enabled. No private signing key, database password or local .env file is tracked.

## Limits

No external gateway, Eureka server, downstream microservice or production ingress was deployed or tested. Downstream resource-server configuration is documented, not claimed as an integration test with a separate service. The issuer is a custom JWT service, not a complete OAuth2/OIDC authorization server. Coordinated multi-key rotation, rate limiting at ingress, monitoring, backups and token-retention scheduling remain deployment work described in ARCHITECTURE.md. Email verification, reset flows and MFA are outside the requested implementation.
