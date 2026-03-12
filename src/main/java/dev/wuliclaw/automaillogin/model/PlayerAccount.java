package dev.wuliclaw.automaillogin.model;

import java.time.Instant;
import java.util.UUID;

public final class PlayerAccount {
    private final UUID uniqueId;
    private final String playerName;
    private String email;
    private String passwordHash;
    private String lastIp;
    private boolean secondFactorVerified;
    private Instant registeredAt;
    private Instant lastLoginAt;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private Instant trustedUntil;
    private Instant lastCodeSentAt;

    public PlayerAccount(UUID uniqueId, String playerName) {
        this.uniqueId = uniqueId;
        this.playerName = playerName;
        this.registeredAt = Instant.now();
    }

    public UUID getUniqueId() { return uniqueId; }
    public String getPlayerName() { return playerName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
    public boolean isSecondFactorVerified() { return secondFactorVerified; }
    public void setSecondFactorVerified(boolean secondFactorVerified) { this.secondFactorVerified = secondFactorVerified; }
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public Instant getTrustedUntil() { return trustedUntil; }
    public void setTrustedUntil(Instant trustedUntil) { this.trustedUntil = trustedUntil; }
    public Instant getLastCodeSentAt() { return lastCodeSentAt; }
    public void setLastCodeSentAt(Instant lastCodeSentAt) { this.lastCodeSentAt = lastCodeSentAt; }
    public boolean hasPassword() { return passwordHash != null && !passwordHash.isBlank(); }
    public boolean isLocked() { return lockedUntil != null && Instant.now().isBefore(lockedUntil); }
    public boolean isTrusted() { return trustedUntil != null && Instant.now().isBefore(trustedUntil); }
}
