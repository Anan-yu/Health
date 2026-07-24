package com.rayk.health.tenant;

import java.util.function.Supplier;

/**
 * 线程级租户上下文，用于在 SecurityContext 不可用时（如异步任务、定时扫描）显式指定当前租户。
 *
 * <p>优先级：TenantContext > SecurityContext。使用完毕后必须调用 {@link #clear()} 防止线程池复用泄漏。
 */
public final class TenantContext {
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> READ_BYPASS = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(long tenantId) {
        HOLDER.set(tenantId);
    }

    public static Long get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static boolean isReadBypassEnabled() {
        return Boolean.TRUE.equals(READ_BYPASS.get());
    }

    /**
     * Executes a read operation without automatic tenant predicates. Callers must still apply
     * patient-level data scope before using this method.
     */
    public static <T> T executeReadWithoutTenant(Supplier<T> action) {
        Boolean previous = READ_BYPASS.get();
        try {
            READ_BYPASS.set(Boolean.TRUE);
            return action.get();
        } finally {
            if (previous == null) {
                READ_BYPASS.remove();
            } else {
                READ_BYPASS.set(previous);
            }
        }
    }

    /**
     * 在指定租户上下文中执行操作，自动清理。
     *
     * @param tenantId 租户 ID
     * @param action 要执行的操作
     */
    public static void execute(long tenantId, Runnable action) {
        try {
            set(tenantId);
            action.run();
        } finally {
            clear();
        }
    }
}
