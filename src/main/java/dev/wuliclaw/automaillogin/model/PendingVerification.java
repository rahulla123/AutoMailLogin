package dev.wuliclaw.automaillogin.model;

import java.time.Instant;

public record PendingVerification(
        String email,
        String code,
        VerificationPurpose purpose,
        Instant expiresAt
) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
