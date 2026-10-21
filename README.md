# EComm-App

E-commerce microservices built with Spring Boot.

## Identity Service

Project: `services/identity-service`. Requires Java 21 and MySQL 8.4. Maven Wrapper is included.

Create a database named `identity_db` and a dedicated MySQL user with schema privileges. Set `DB_PASSWORD` securely. Optional configuration:

| Variable | Default |
| --- | --- |
| DB_URL | jdbc:mysql://localhost:3306/identity_db |
| DB_USERNAME | identity |
| PORT | 8081 |

```sh
cd services/identity-service
./mvnw clean verify
./mvnw spring-boot:run
```

Tests require a running Docker daemon and start an isolated MySQL container. They exercise Flyway migrations, Hibernate schema validation, shared profile IDs, seeded roles, and relationship persistence.

For restricted cloud machines, use a writable Maven cache:

```sh
MAVEN_USER_HOME=/workspace/.m2 ./mvnw -Dmaven.repo.local=/workspace/.m2/repository clean verify
```

### Entities

- User: login email, password hash, account status, timestamps.
- Profile: personal information; `user_id` is both primary key and foreign key to User.
- Role: shared CUSTOMER and ADMIN records seeded by Flyway.
- UserRole: explicit join entity with composite primary key; many users can share roles.
- Address: multiple addresses per user.
- RefreshToken: multiple sessions per user; store only SHA-256 token hashes.

Relationships are unidirectional and lazy, with no cascading deletes. Create User and Profile in one transaction. The schema guarantees at most one profile per user; registration must ensure a profile exists. Normalize email before saving, encode passwords using Spring Security, and use DTOs for API responses. Public registration must assign CUSTOMER itself.

Flyway owns schema changes; Hibernate uses `ddl-auto=validate`. Keep database credentials and future signing keys outside source control.

This project contains entities and persistence validation only. Authentication, registration, controllers, token rotation, and authorization are future work. There are no business API endpoints yet.
