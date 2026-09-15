package com.aiecomm.camp.modules.commerce.context;

import java.util.Optional;

/**
 * ThreadLocal storage for StoreContext.
 * Populated by StorefrontContextFilter and cleared in its finally block.
 */
public final class StoreContextHolder {

    private static final ThreadLocal<StoreContext> CONTEXT = new ThreadLocal<>();

    private StoreContextHolder() {}

    public static void set(StoreContext context) {
        CONTEXT.set(context);
    }

    public static Optional<StoreContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static StoreContext require() {
        StoreContext ctx = CONTEXT.get();
        if (ctx == null) {
            throw new IllegalStateException("No StoreContext present on current thread");
        }
        return ctx;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
