package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PendingVerification;
import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import dev.wuliclaw.automaillogin.storage.StorageProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class VerificationService {
    private final AutoMailLoginPlugin plugin;
    private final SecureRandom secureRandom = new SecureRandom();
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
        PlayerAccount account = storageProvider.findByUniqueId(uniqueId).orElse(null);
        if (account != null) {
            account.setPendingCode(hashCode(code));
            account.setPendingEmail(email);
            account.setPendingPurpose(purpose.name());
            account.setPendingExpiresAt(verification.expiresAt());
            account.setPendingFailedAttempts(0);
            account.setPendingLockedUntil(null);
            storageProvider.save(account);
        }
        return verification;
    }

    public boolean canSend(UUID uniqueId, VerificationPurpose purpose) {
        int cooldown = getCooldownSeconds(purpose);
        if (cooldown <= 0) {
            return true;
        }
        PlayerAccount account = storageProvider.findByUniqueId(uniqueId).orElse(null);
        if (account == null || account.getLastCodeSentAt() == null) {
            return true;
        }
        return Duration.between(account.getLastCodeSentAt(), Instant.now()).getSeconds() >= cooldown;
    }

    public long getRemainingCooldownSeconds(UUID uniqueId, VerificationPurpose purpose) {
        int cooldown = getCooldownSeconds(purpose);
        PlayerAccount account = storageProvider.findByUniqueId(uniqueId).orElse(null);
        if (account == null || account.getLastCodeSentAt() == null) {
            return 0;
        }
        long elapsed = Duration.between(account.getLastCodeSentAt(), Instant.now()).getSeconds();
        return Math.max(0, cooldown - elapsed);
    }

    public boolean verify(UUID uniqueId, String code, VerificationPurpose purpose) {
        PlayerAccount account = storageProvider.findByUniqueId(uniqueId).orElse(null);
        if (account == null || account.getPendingCode() == null || account.getPendingPurpose() == null || account.getPendingExpiresAt() == null) {
            return false;
        }
        if (account.isPendingLocked()) {
            return false;
        }
        if (Instant.now().isAfter(account.getPendingExpiresAt())) {
            account.clearPendingVerification();
            storageProvider.save(account);
            return false;
        }
        if (!purpose.name().equals(account.getPendingPurpose())) {
            return false;
        }

        boolean success = account.getPendingCode().equals(hashCode(code));
        if (success) {
            account.clearPendingVerification();
            storageProvider.save(account);
            return true;
        }

        int maxAttempts = plugin.getConfig().getInt("mail.max-verify-attempts", 5);
        int lockSeconds = plugin.getConfig().getInt("mail.verify-lock-seconds", 300);
        account.setPendingFailedAttempts(account.getPendingFailedAttempts() + 1);
        if (maxAttempts > 0 && account.getPendingFailedAttempts() >= maxAttempts) {
            account.setPendingFailedAttempts(0);
            account.setPendingLockedUntil(Instant.now().plusSeconds(lockSeconds));
        }
        storageProvider.save(account);
        return false;
    }

    public PendingVerification get(UUID uniqueId) {
        PlayerAccount account = storageProvider.findByUniqueId(uniqueId).orElse(null);
        if (account == null || account.getPendingCode() == null || account.getPendingPurpose() == null || account.getPendingExpiresAt() == null) {
            return null;
        }
        try {
            return new PendingVerification(account.getPendingEmail(), "******", VerificationPurpose.valueOf(account.getPendingPurpose()), account.getPendingExpiresAt());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public long getRemainingVerifyLockSeconds(UUID uniqueId) {
        PlayerAccount account = storageProvider.findByUniqueId(uniqueId).orElse(null);
        if (account == null || account.getPendingLockedUntil() == null) {
            return 0;
        }
        long remaining = Duration.between(Instant.now(), account.getPendingLockedUntil()).getSeconds();
        return Math.max(0, remaining);
    }

    private int getCooldownSeconds(VerificationPurpose purpose) {
        return switch (purpose) {
            case REGISTER -> plugin.getConfig().getInt("mail.resend-cooldown-register-seconds", plugin.getConfig().getInt("mail.resend-cooldown-seconds", 60));
            case RESET_PASSWORD -> plugin.getConfig().getInt("mail.resend-cooldown-reset-seconds", plugin.getConfig().getInt("mail.resend-cooldown-seconds", 60));
            case SECOND_FACTOR -> plugin.getConfig().getInt("mail.resend-cooldown-second-factor-seconds", plugin.getConfig().getInt("mail.resend-cooldown-seconds", 60));
        };
    }

    private String generateCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }

    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
