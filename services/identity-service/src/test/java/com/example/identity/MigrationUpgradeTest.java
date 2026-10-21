package com.example.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class MigrationUpgradeTest {
  @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

  @Test
  void upgradePreservesExistingAccountsProfilesAddressesAndRoles() {
    var source =
        new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    Flyway.configure().dataSource(source).target("1").load().migrate();
    var jdbc = new JdbcTemplate(source);
    jdbc.update(
        "insert into users (id,email,password_hash,status,created_at,updated_at) values"
            + " (1,'existing@example.test','existing-hash','DISABLED',NOW(),NOW())");
    jdbc.update(
        "insert into profiles (user_id,first_name,last_name,updated_at) values"
            + " (1,'Existing','Customer',NOW())");
    jdbc.update(
        "insert into addresses"
            + " (user_id,recipient_name,phone,line1,city,state,postal_code,country_code) values"
            + " (1,'Existing','123','Street','City','State','001','IN')");
    jdbc.update(
        "insert into user_roles (user_id,role_id) select 1,id from roles where name='CUSTOMER'");
    jdbc.update(
        "insert into refresh_tokens (user_id,token_hash,expires_at,created_at) values"
            + " (1,?,DATE_ADD(NOW(), INTERVAL 1 DAY),NOW())",
        "a".repeat(64));
    Flyway.configure().dataSource(source).load().migrate();
    assertThat(jdbc.queryForObject("select full_name from users where id=1", String.class))
        .isEqualTo("Existing Customer");
    assertThat(jdbc.queryForObject("select password_hash from users where id=1", String.class))
        .isEqualTo("existing-hash");
    assertThat(jdbc.queryForObject("select status from users where id=1", String.class))
        .isEqualTo("INACTIVE");
    assertThat(
            jdbc.queryForObject(
                "select r.name from roles r join user_roles ur on ur.role_id=r.id where"
                    + " ur.user_id=1",
                String.class))
        .isEqualTo("ROLE_USER");
    assertThat(jdbc.queryForObject("select count(*) from profiles where user_id=1", Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from addresses where user_id=1", Long.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select revoked from refresh_tokens where user_id=1", Boolean.class))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "select family_id from refresh_tokens where user_id=1", String.class))
        .isNotBlank();
    // A repeated migration is a no-op rather than resetting user data.
    assertThat(Flyway.configure().dataSource(source).load().migrate().migrationsExecuted).isZero();
  }
}
