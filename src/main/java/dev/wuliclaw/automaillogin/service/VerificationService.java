package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PendingVerification;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VerificationService {
    private final AutoMailLoginPlugin plugin;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<UUID, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();

    public VerificationService(AutoMailLoginPlugin plugin) {
        this.plugin = plugin;
    }

    public PendingVerification create(UUID uniqueId, String email, VerificationPurpose purpose) {
        int codeLength = plugin.getConfig().getInt("mail.code-length", 6);
        int expireSeconds = plugin.getConfig().getInt("mail.code-expire-seconds", 300);
        String code = generateCode(codeLength);
        PendingVerification verification = new PendingVerification(email, code, purpose, Instant.now().plusSeconds(expireSeconds));
        pendingVerifications.put(uniqueId, verification);
        return verification;
    }

    public boolean verify(UUID uniqueId, String code, VerificationPurpose purpose) {
        PendingVerification verification = pendingVerifications.get(uniqueId);
        if (verification == null || verification.isExpired() || verification.purpose() != purpose) {
            return false;
        }
        boolean success = verification.code().equals(code);
        if (success) {
            pendingVerifications.remove(uniqueId);
        }
        return success;
    }

    public PendingVerification get(UUID uniqueId) {
        return pendingVerifications.get(uniqueId);
    }

    private String generateCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }
}
