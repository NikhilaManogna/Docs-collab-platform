package com.nkh.document.security;

public final class SecurityContextHolder {

    private static final ThreadLocal<AuthenticatedUser> CONTEXT = new ThreadLocal<>();

    private SecurityContextHolder() {
    }

    public static void set(AuthenticatedUser user) {
        CONTEXT.set(user);
    }

    public static AuthenticatedUser get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
