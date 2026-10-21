package com.ecomm.identity;

import com.ecomm.identity.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class IdentityPersistenceTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired EntityManager em;

    @Test
    void persistsIdentityRelationshipsAndSeededRoles() {
        User user = new User();
        user.setEmail("customer@example.com");
        user.setPasswordHash("test-only-encoded-password");
        em.persist(user);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setFirstName("Punith");
        em.persist(profile);

        Role customer = em.createQuery("select r from Role r where r.name = :name", Role.class)
                .setParameter("name", RoleName.CUSTOMER).getSingleResult();
        UserRole assignment = new UserRole();
        assignment.setId(new UserRoleId(user.getId(), customer.getId()));
        assignment.setUser(user);
        assignment.setRole(customer);
        em.persist(assignment);

        Address address = new Address();
        address.setUser(user);
        address.setRecipientName("Punith");
        address.setPhone("1234567890");
        address.setLine1("123 Example Street");
        address.setCity("Bengaluru");
        address.setState("Karnataka");
        address.setPostalCode("560001");
        address.setCountryCode("IN");
        em.persist(address);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash("a".repeat(64));
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        em.persist(token);
        em.flush();
        Long userId = user.getId();
        Long addressId = address.getId();
        Long tokenId = token.getId();
        em.clear();

        assertThat(em.find(Profile.class, userId).getUser().getId()).isEqualTo(userId);
        assertThat(em.find(User.class, userId).getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(em.find(UserRole.class, new UserRoleId(userId, customer.getId())).getRole().getName())
                .isEqualTo(RoleName.CUSTOMER);
        assertThat(em.find(Address.class, addressId).getUser().getId()).isEqualTo(userId);
        assertThat(em.find(RefreshToken.class, tokenId).getUser().getId()).isEqualTo(userId);
        assertThat(em.createQuery("select count(r) from Role r", Long.class).getSingleResult()).isEqualTo(2L);
    }
}
