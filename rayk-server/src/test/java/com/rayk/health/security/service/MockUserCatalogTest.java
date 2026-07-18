package com.rayk.health.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class MockUserCatalogTest {
    @Test
    void allDevelopmentPasswordsAreBcryptAndMatch() {
        MockUserCatalog catalog = new MockUserCatalog();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertThat(catalog.all())
                .allSatisfy(
                        account -> {
                            assertThat(account.passwordHash()).startsWith("$2");
                            assertThat(encoder.matches("RayK@123456", account.passwordHash())).isTrue();
                        });
    }
}

