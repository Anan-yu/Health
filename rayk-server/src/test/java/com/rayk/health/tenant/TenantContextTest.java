package com.rayk.health.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {
    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setAndGetReturnsTenantId() {
        TenantContext.set(20001L);
        assertThat(TenantContext.get()).isEqualTo(20001L);
    }

    @Test
    void clearRemovesTenantId() {
        TenantContext.set(20001L);
        TenantContext.clear();
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void getReturnsNullWhenNotSet() {
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void executeSetsAndClearsTenantId() {
        AtomicLong captured = new AtomicLong(-1);
        TenantContext.execute(
                20002L,
                () -> {
                    Long value = TenantContext.get();
                    if (value != null) {
                        captured.set(value);
                    }
                });
        assertThat(captured.get()).isEqualTo(20002L);
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void executeClearsEvenOnException() {
        try {
            TenantContext.execute(
                    20001L,
                    () -> {
                        throw new RuntimeException("模拟异常");
                    });
        } catch (RuntimeException ignored) {
            // expected
        }
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void tenantContextIsThreadLocal() throws InterruptedException {
        TenantContext.set(20001L);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong otherThreadValue = new AtomicLong(-1);
        Thread other =
                new Thread(
                        () -> {
                            Long value = TenantContext.get();
                            otherThreadValue.set(value == null ? -1 : value);
                            latch.countDown();
                        });
        other.start();
        latch.await();
        assertThat(otherThreadValue.get()).isEqualTo(-1);
        assertThat(TenantContext.get()).isEqualTo(20001L);
    }
}
