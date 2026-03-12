package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PendingVerification;
import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import dev.wuliclaw.automaillogin.storage.StorageProvider;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VerificationService {
    private final AutoMailLoginPlugin plugin;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<UUID, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();
    private final StorageProvider storageProvider;

    public VerificationService(AutoMailLoginPlugin plugin, StorageProvider storageProvider) {
        this.plugin = plugin;
        this.storageProvider = storageProvider;
    }

    public PendingVerification create(UUID uniqueId, String email, VerificationPurpose purpose) {
        int codeLength = plugin.getConfig().getInt("mail.code-length", 6);
        int expireSeconds = plugin.getConfig().getInt("mail.code-expire-seconds", 300);
        String code = generateCode(codeLength);
        PendingVerification verification = new PendingVerification(email, code, purpose, Instant.now().plusSeconds(expireSeconds));
        pendingVerifications.put(uniqueId, verification);
        return verification;
    }

    public boolean canSend(UUID uniqueId) {
        int cooldown = plugin.getConfig().getInt("mail.resend-cooldown-seconds", 60);
        if (cooldown <= 0) {
            return true;
        }
        PlayerAccount account = storageProvider.findByUniqueId(uniqueId).orElse(null);
        if (account == null || account.getLastCodeSentAt() == null) {
            return true;
        }
        return Duration.between(account.getLastCodeSentAt(), Instant.now()).getSeconds() >= cooldown;
    }

    public long getRemainingCooldownSeconds(UUID uniqueId) {
        int cooldown = plugin.getConfig().getInt("mail.resend-cooldown-seconds", 60);
        PlayerAccount account = storageProvider.findByUniqueId(uniqueId).orElse(null);
        if (account == null || account.getLastCodeSentAt() == null) {
            return 0;
        }
        long elapsed = Duration.between(account.getLastCodeSentAt(), Instant.now()).getSeconds();
        return Math.max(0, cooldown - elapsed);
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
