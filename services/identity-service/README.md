# Identity Service

Independent identity API under `com.example.identity`, built for Java 17, Spring Boot 4.1.1, Spring Security 7.1.1, MySQL 8.4 and Springdoc 3.1.1. Maven Wrapper is included. Dependencies such as Lombok, Nimbus JOSE/JWT, Mockito and Testcontainers use Boot's managed versions.

**Compatibility note:** Spring Boot 4.1's test framework requires JUnit 6 APIs. Forcing JUnit 5 causes SpringExtension to fail before integration tests start. This project therefore uses Boot-managed JUnit 6.0.3 and Jupiter tests. Java 17 remains the build target and Docker runtime.

## Run with Docker Compose

Requirements: Docker with Compose, OpenSSL, and unused local ports 8081 and 3306.

```sh
cd services/identity-service
cp .env.example .env
# Replace both database-password placeholders in .env with different random values.
./scripts/generate-dev-keys.sh
# Set APP_UID and APP_GID in .env to the IDs printed by the script.
docker compose up --build -d
docker compose logs -f identity
```

The app starts after the MySQL health check succeeds. Check readiness by requesting OpenAPI JSON and exercising registration/login below. Local service addresses:

```text
Swagger UI: http://localhost:8081/swagger-ui/index.html
OpenAPI:    http://localhost:8081/v3/api-docs
Public keys:http://localhost:8081/.well-known/jwks.json
```

These are addresses for your own machine, not hosted deployment links. Use HTTPS and a stable issuer URL in production. The container connects to MySQL using service name `mysql`, not localhost. Application and database ports bind only to local loopback for development. The app runs as a non-root user; the mounted private key must be readable by that UID. Never make private keys world-readable.

```sh
docker compose down
# Volumes retain database state. Removing volumes deletes that local database.
```

Changing MYSQL_PASSWORD or MYSQL_ROOT_PASSWORD does not change credentials in an already-initialized MySQL volume. Rotate them with database administration commands rather than assuming an environment edit updates existing users.

## Run locally with Maven

Install a full JDK 17+ (not just a JRE). Maven compiles with `--release 17`. Tests require Docker and run against disposable MySQL containers, independently of your development database.

```sh
cd services/identity-service
./mvnw clean verify
./scripts/generate-dev-keys.sh  # once; refuses to overwrite existing keys
```

Prepare `identity_db` in MySQL 8 and a dedicated schema user. Export these settings securely in your shell or IDE; never commit credentials:

```text
DB_URL=jdbc:mysql://localhost:3306/identity_db?connectionTimeZone=UTC
DB_USERNAME=identity
DB_PASSWORD=<your local database password>
SPRING_PROFILES_ACTIVE=dev
```

Then run:

```sh
./mvnw spring-boot:run
# Or after packaging:
java -jar target/identity-service-0.1.0-SNAPSHOT.jar
```

`.env` is read by Compose, not automatically by Spring Boot or Maven. Set local process variables explicitly. To start only the Compose database for local Java development:

```sh
docker compose up -d mysql
```

The local Java process must use the same database password configured for that database.

## Configuration

| Variable | Default / purpose |
| --- | --- |
| DB_URL | Local MySQL URL; mandatory in prod profile |
| DB_USERNAME | identity locally; mandatory in prod |
| DB_PASSWORD | Required, no hardcoded default |
| JWT_PRIVATE_KEY_LOCATION | file:./.local/keys/private.pem; required explicitly in prod |
| JWT_PUBLIC_KEY_LOCATION | file:./.local/keys/public.pem; required explicitly in prod |
| JWT_ISSUER | http://localhost:8081 locally; required in prod |
| JWT_AUDIENCE | ecomm-api locally; required in prod |
| JWT_KEY_ID | identity-1 locally; required in prod |
| JWT_ACCESS_TTL | PT15M, positive and at most one hour |
| JWT_REFRESH_TTL | P7D, longer than access TTL |
| CORS_ALLOWED_ORIGINS | Comma-separated exact trusted origins; none by default |
| PORT | 8081 |

Dev profile allows localhost React origins. Prod profile requires explicit key/database/issuer settings. Deploy behind a TLS ingress; configure production DB_URL with certificate-verified TLS for a remote MySQL server. Flyway creates/upgrades the schema; Hibernate uses validate. Keys must be matching RSA PEMs, at least 2048 bits; the dev script generates 3072-bit keys. Secrets and private keys never belong in images or Git.

## API examples

Register:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "fullName": "Punith B",
  "email": "punith@example.com",
  "password": "Example-Password-123!"
}
```

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "punith@example.com",
  "password": "Example-Password-123!"
}
```

Both return `{message,data,httpStatus}`; data contains accessToken, refreshToken, tokenType, expiresIn and a safe user object. Passwords must have at least 12 characters and at most 72 UTF-8 bytes. Registration always assigns ROLE_USER; extra input fields are rejected.

Authenticated request:

```sh
curl --fail-with-body http://localhost:8081/api/v1/users/me \
  -H 'Authorization: Bearer <accessToken>'
```

Refresh/logout bodies:

```json
{"refreshToken":"<current-refresh-token>"}
```

Refresh rotates the token. Replace your stored token after every successful refresh. Reusing an old token revokes its family, including its successor. Logout revokes the family but does not invalidate already-issued access JWTs; they expire normally. Do not send concurrent refreshes from a client.

| Method | Endpoint | Access |
| --- | --- | --- |
| POST | /api/v1/auth/register | Public |
| POST | /api/v1/auth/login | Public |
| POST | /api/v1/auth/refresh | Refresh-token possession |
| POST | /api/v1/auth/logout | Refresh-token possession, idempotent |
| POST | /api/v1/auth/validate | Admin bearer token; body `{ "token": "<JWT>" }` |
| GET | /api/v1/users/me | Authenticated |
| GET | /api/v1/users/{id} | Owner or admin |
| GET | /api/v1/users?page=0&size=20 | Admin; max size 100 |
| PATCH | /api/v1/users/{id}/status | Admin; body `{ "status": "LOCKED" }` |
| GET | /.well-known/jwks.json | Public verification key only |

Account statuses: ACTIVE, INACTIVE, LOCKED. Status changes to INACTIVE/LOCKED revoke all refresh sessions and stop bearer access within Identity Service. Downstream offline JWT validation sees such changes only through expiry or a separate revocation mechanism.

### Bootstrap an administrator

Register the intended administrator normally, verify the account's ownership through your operational process, and use a trusted database administration connection to grant ROLE_ADMIN:

```sql
-- Replace the example email with the verified account. Run only through trusted administration.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.email = 'verified-admin@example.com' AND r.name = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);
```

Then log in again so newly issued JWTs include the admin authority. No default administrator password or public role-assignment API is provided.

## Tests and implementation explanation

```sh
./mvnw clean verify
./mvnw -Dtest=JwtServiceTest,UserServiceTest test  # unit tests only, no Docker
```

To smoke-test a running development instance with Python 3:

```sh
python3 scripts/smoke-test.py http://localhost:8081
```

This creates a new test customer and prints only check results, not credentials or tokens.

Integration tests do not silently skip without Docker. They cover registration, duplicate email, BCrypt, login failures, JWT expiry/signature/issuer/audience/algorithm, ownership/admin rules, refresh rotation/replay/concurrency, logout, unavailable accounts, input validation, CORS, Swagger, public keys, and upgrading a populated V1 database. See [validation evidence](docs/VALIDATION.md) for the executed checks and limitations.

The complete [architecture walkthrough](docs/ARCHITECTURE.md) explains all packages, important methods in execution order, database relationships, Swagger testing, and resource-server configuration for downstream microservices.

## Production deployment scope

Docker support and security tests establish a working implementation, not a production deployment. Add TLS ingress, login/registration/refresh rate limits, monitoring, backups, a token-retention job and coordinated signing-key rotation to your deployment. The JWKS currently publishes one public key. This custom token issuer does not implement a complete OAuth2/OIDC authorization server. Email ownership verification, password reset, MFA, and external discovery registration are outside this task.
