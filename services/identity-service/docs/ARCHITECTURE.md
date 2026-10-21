# Identity Service: code and request walkthrough

## Package structure

```text
com.example.identity
├── IdentityServiceApplication
├── config        SecurityConfig, JwtKeyConfig, JwtProperties, OpenApiConfig
├── controller    AuthController, UserController, JwkController
├── dto
│   ├── request   RegisterRequest, LoginRequest, RefreshTokenRequest,
│   │             ValidateTokenRequest, UpdateStatusRequest
│   └── response  ApiResponse, AuthResponse, UserResponse,
│                 UserPageResponse, TokenValidationResponse
├── entity        User, Role, RefreshToken, Profile, Address
├── enums         AccountStatus, RoleName
├── repository    UserRepository, RoleRepository, RefreshTokenRepository
├── service       AuthService, UserService, JwtService
│   └── impl      AuthServiceImpl, UserServiceImpl, JwtServiceImpl
├── security      IdentityPrincipal, CustomUserDetailsService,
│                 JwtAuthenticationFilter, CustomAuthenticationEntryPoint,
│                 CustomAccessDeniedHandler
├── exception     ApplicationException, ResourceNotFoundException,
│                 InvalidRefreshTokenException, GlobalExceptionHandler
└── mapper        UserMapper
```

Controllers translate HTTP requests into service calls. Validated request DTOs define allowed input fields; response DTOs prevent entities, password hashes and lazy relationships from leaking into JSON. Services own transactions and business rules. Repositories own database access. Entities map persistence state. Security classes establish identity and permissions before business methods execute. UserMapper performs explicit output mapping.

## Database and evolution from the entity scaffold

User and Role use a unidirectional many-to-many association through `user_roles`. The join table retains its composite primary key; a separate UserRole Java entity is no longer necessary for the requested mapping. Roles have no cascade deletion. The original `users.id` and `roles.id` columns are retained and mapped as `userId` and `roleId` in Java.

Profile and Address remain in the same database to preserve the existing scaffold. Profile has a shared primary key with User; Address and RefreshToken each have a many-to-one association to User. Registration creates a Profile in the same transaction. `User.fullName` is the account display name required by this API; Profile holds personal details for future profile APIs. No profile-edit or address endpoints are introduced here. Do not treat the initial split of fullName as a culturally complete name parser.

V1 is unchanged. V2 backfills fullName from profiles (falling back to email), maps DISABLED to INACTIVE, renames CUSTOMER/ADMIN to ROLE_USER/ROLE_ADMIN and retains role assignments. Existing refresh tokens are revoked during this security upgrade. It adds token families, revocation state and indexes. The historical revoked_at column is retained for migration compatibility; the new lifecycle uses `revoked`. Hibernate validates rather than mutates the schema. All timestamps use UTC. Flyway upgrade tests cover preservation of populated V1 databases.

## Registration, in execution order

1. SecurityConfig permits POST `/api/v1/auth/register` without authentication.
2. AuthController validates RegisterRequest. Unknown JSON fields are rejected; clients cannot supply roles, accountStatus, IDs or timestamps.
3. AuthServiceImpl.register checks BCrypt's 72-byte UTF-8 limit and normalizes email using strip and Locale.ROOT lowercase. A unique database constraint also handles concurrent registrations.
4. PasswordEncoder produces a salted BCrypt hash at cost 12. Passwords are hashed, not reversibly encrypted.
5. The service assigns only ROLE_USER from the migrated roles table. It persists User and Profile atomically.
6. JwtServiceImpl signs an access token. A cryptographically random 256-bit refresh token is generated; only its SHA-256 hash, expiry, family ID and revocation state are stored.
7. AuthResponse returns the raw refresh token once, the access token, Bearer type, expiry in seconds, and a safe UserResponse. Spring Security adds no-cache response headers.

## Login flow

```text
Client → AuthController.login → AuthServiceImpl.login
       → AuthenticationManager (ProviderManager)
       → DaoAuthenticationProvider → CustomUserDetailsService
       → UserRepository → MySQL
       → account checks + BCrypt verification
       → lock/recheck account → JWT + refresh token → AuthResponse → Client
```

CustomUserDetailsService loads the normalized email and roles, and creates IdentityPrincipal. DaoAuthenticationProvider rejects unknown users, invalid passwords, locked accounts and inactive accounts. PasswordEncoder.matches compares the submitted password with the salted hash. User-facing authentication failures use a generic message. Login then locks and refreshes the account state before creating a new token family, preventing a concurrent disabling operation from being silently ignored.

## Access-token generation and verification

JwtKeyConfig reads a PKCS#8 private PEM and X.509 public PEM from configurable resources. Startup fails for unreadable, mismatched or undersized RSA keys. No private key is checked into Git or generated automatically in production.

JwtServiceImpl.generateAccessToken emits an RS256-signed JWT with `sub` (numeric user ID), `email`, `roles`, `iat`, `exp`, `iss`, `aud`, `jti` and `token_use=access`. The header includes `kid`. Nimbus JOSE/JWT is supplied through Spring Security's maintained OAuth2 JOSE module.

The decoder accepts RS256 only and verifies signature, issuer, audience, required claims and timestamps. Clock skew is 30 seconds. Access lifetime defaults to 15 minutes and is constrained to at most one hour. JwtService extraction methods always validate the token first; they never trust a decoded unsigned payload.

## Protected request flow

```text
Authorization: Bearer <access-token>
  → SecurityFilterChain / CORS
  → JwtAuthenticationFilter
  → JwtService.validateAccessToken
  → CustomUserDetailsService.loadById (current account + roles)
  → account status checks
  → SecurityContext populated with authenticated IdentityPrincipal
  → request authorization rules
  → controller → method authorization on UserService
  → service transaction → repository → MySQL → mapped DTO
```

JwtAuthenticationFilter runs before UsernamePasswordAuthenticationFilter. Invalid bearer tokens receive JSON 401 through CustomAuthenticationEntryPoint. The filter does not register separately as a servlet filter, so it executes once. Missing bearer tokens remain unauthenticated; protected routes then return 401. An authenticated user lacking permission receives JSON 403 through the access-denied handler or advice.

Within Identity Service, account state and authorities are read from MySQL on every bearer request. Disabling/locking an account therefore blocks its existing access tokens here immediately. Method rules restrict `getUserById` to the owner or an admin, and listing/status changes to admins. Pagination is capped at 100 users. The validation endpoint requires an authenticated admin, and independently verifies the submitted token and target account. It never establishes a caller identity from the body token.

HTTP Basic, form login, default logout and server-side sessions are disabled. CSRF is disabled because credentials are carried explicitly in bearer headers or JSON refresh requests, not ambient authentication cookies. If authentication is moved to cookies, revisit CSRF protections. CORS permits only configured exact origins and does not allow credentials.

## Refresh rotation, replay and logout

Each login/registration starts a token family. Refresh hashes the submitted value, finds its account ID without loading a stale token entity, locks the account, then locks the token row. It checks active status, expiry and revocation, revokes the old token and inserts a replacement in the same family and transaction. Clients must serialize refresh requests and replace the stored refresh token after every successful refresh.

Reusing a revoked token revokes every token in that family, including any successor. This is deliberate replay detection: even concurrent requests cannot obtain two live successors. InvalidRefreshTokenException is excluded from transaction rollback so replay revocation actually commits. Unrelated login sessions have different families. Expired tokens are also rejected. Repository cleanup methods are provided; an operational retention job should retain replay evidence for an appropriate window rather than deleting rows immediately on logout.

Logout accepts a refresh token and idempotently revokes its family without requiring an unexpired access token. It does not revoke other devices' independent families. Existing access JWTs remain valid until expiry (including 30 seconds of permitted skew); downstream services verifying locally have the same behavior. Disabling an account also revokes all its refresh tokens. Identity's own account check blocks disabled users immediately, but offline downstream validation does not learn account changes immediately.

## Errors and safe output

GlobalExceptionHandler maps duplicate/conflicting records to 409, invalid authentication/tokens to 401, forbidden operations to 403, missing users to 404, malformed/invalid input to 400 and unexpected failures to generic 500 responses. Validation errors report field names/messages without submitted values. Responses use `{message,data,httpStatus}`. No response returns password hashes, token hashes or signing secrets. Server logging avoids raw exception messages and request bodies containing credentials.

## Swagger testing sequence

1. Start MySQL and the service; open `/swagger-ui/index.html` on your local service.
2. Expand register and submit fullName, email and a password of at least 12 characters and at most 72 UTF-8 bytes. Copy data.accessToken and data.refreshToken privately.
3. Click Authorize and paste the access token (Swagger adds the Bearer prefix).
4. Execute GET `/api/v1/users/me` and owner GET by ID. A regular user receives 403 for the user listing and validation endpoint.
5. Execute login to create an independent session, or refresh with the refreshToken body. Save the replacement refresh token.
6. Execute logout with the current refresh token. A later refresh fails; the access JWT still works until expiry.
7. To test admin operations, provision an admin through the trusted database procedure in README, then log in again. No public role-management API exists.

OpenAPI JSON is `/v3/api-docs`. Protected operations declare bearerAuth; registration/login/refresh/logout/public keys require no JWT. Request/response DTO schemas and common validation/security error codes appear in the specification. Swagger authorization is not persisted across reloads.

## Downstream microservice verification

Expose the service only through HTTPS in production. Other services fetch public keys from `/.well-known/jwks.json` and use Spring Security OAuth2 Resource Server. They never receive the private key and need not call Identity for every request.

Example decoder bean in a downstream service (using its resource-server dependencies):

```java
@Bean
JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder
        .withJwkSetUri("https://identity.example.com/.well-known/jwks.json")
        .jwsAlgorithm(SignatureAlgorithm.RS256).build();
    OAuth2TokenValidator<Jwt> audience = token ->
        token.getAudience().contains("ecomm-api")
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer("https://identity.example.com"), audience));
    return decoder;
}
```

Configure a JwtGrantedAuthoritiesConverter with authorities claim `roles` and empty prefix because claim values already contain ROLE_. Authorize each service's endpoints independently. Enforce access-token purpose/required claims as needed by the consumer contract. This is a custom bearer-token issuer, not a complete OAuth2/OIDC authorization server: no discovery document, consent or authorization-code flow is claimed.

The gateway can route `/api/v1/auth/**`, `/api/v1/users/**` and public keys. Eureka registration is a future deployment concern, not required for this standalone service. Product, Order and Payment should store user IDs as external references and verify tokens independently. Profile remains local for now and can be extracted later using an explicit data-migration/API/event contract.

## Deployment boundary and follow-up operations

The Compose stack is for local development: MySQL and HTTP ports bind to loopback, credentials are user-provided, keys are mounted read-only, and the application runs without root privileges. Production needs a TLS ingress, trusted origin configuration, managed secrets, backups, monitoring, rate limits for registration/login/refresh, and an operational token-cleanup policy. A separate migration principal can run Flyway before starting the application with a least-privilege runtime database account.

The current JWKS serves one key. Coordinated overlapping-key publication and key rotation are not implemented. Plan rotation before production rollout: publish old and new public keys during the overlap and retain old verification keys until their tokens expire. For immediate cross-service revocation use an explicit introspection/revocation design rather than assuming offline JWT validation detects logout or status changes.

## Dependency compatibility

The selected stable set is Spring Boot 4.1.1, Spring Security 7.1.1, Springdoc 3.1.1, and Java 17 bytecode/runtime. Boot manages the compatible Nimbus, Hibernate, Lombok, Mockito and Testcontainers versions. Boot 4.1's Spring TestContext calls JUnit 6 APIs; overriding it to requested JUnit 5.14.4 produced NoSuchMethodError in SpringExtension before any integration test ran. Therefore the project uses Boot-managed JUnit 6.0.3 with Jupiter tests. This is the explicit compatibility departure from the original stack request.
