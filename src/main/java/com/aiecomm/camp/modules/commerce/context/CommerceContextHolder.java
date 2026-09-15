package com.aiecomm.camp.modules.commerce.context;

/**
 * ThreadLocal holder for the CommerceContext resolved during the current request.
 * Set by StorefrontContextFilter, cleared in its finally block.
 */
public final class CommerceContextHolder {

    private static final ThreadLocal<CommerceContext> holder = new ThreadLocal<>();

    private CommerceContextHolder() {}

    public static void set(CommerceContext ctx) {
        holder.set(ctx);
    }

    public static CommerceContext get() {
        CommerceContext ctx = holder.get();
        if (ctx == null) {
            throw new IllegalStateException(
                "CommerceContext not set on current thread. " +
                "Ensure StorefrontContextFilter ran before accessing commerce context.");
        }
        return ctx;
    }

    public static boolean isSet() {
        return holder.get() != null;
    }

    public static void clear() {
        holder.remove();
    }
}
