package dev.wuliclaw.automaillogin.security;

public enum SecondFactorMode {
    DISABLED,
    ALWAYS,
    ON_NEW_IP,
    ON_PASSWORD_RESET,
    ON_ADMIN_FORCE,
    MIXED;

    public static SecondFactorMode fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return ON_NEW_IP;
        }
        try {
            return SecondFactorMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ON_NEW_IP;
        }
    }
}
