package com.tainted.authuser.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserAccountRepository @DataJpaTest (Testcontainers MySQL — 실제 DB 방언/제약 검증).
 * 커스텀 파생 쿼리(findBySocialProviderAndExternalId)와 (social_provider, external_id)
 * 유니크 제약을 실제 MySQL 에서 확인한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserAccountRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4").withDatabaseName("authuser");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired UserAccountRepository repository;

    @Test
    void findBySocialProviderAndExternalId_returnsMatch() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        repository.save(new UserAccount("u-1", "kakao", "kakao:100", now));

        Optional<UserAccount> found =
                repository.findBySocialProviderAndExternalId("kakao", "kakao:100");

        assertTrue(found.isPresent());
        assertEquals("u-1", found.get().getId());
        assertEquals("kakao", found.get().getSocialProvider());
    }

    @Test
    void findBySocialProviderAndExternalId_noMatchIsEmpty() {
        assertTrue(repository.findBySocialProviderAndExternalId("naver", "naver:none").isEmpty());
    }

    @Test
    void findBySocialProviderAndExternalId_discriminatesByProvider() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        repository.save(new UserAccount("g-1", "google", "shared-ext", now));

        // 같은 externalId 라도 provider 가 다르면 매칭되지 않음.
        assertTrue(repository.findBySocialProviderAndExternalId("kakao", "shared-ext").isEmpty());
        assertTrue(repository.findBySocialProviderAndExternalId("google", "shared-ext").isPresent());
    }

    @Test
    void uniqueConstraintOnProviderAndExternalIdEnforced() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        repository.saveAndFlush(new UserAccount("a", "naver", "dup", now));

        assertThrows(DataIntegrityViolationException.class, () ->
                repository.saveAndFlush(new UserAccount("b", "naver", "dup", now)));
    }
}
