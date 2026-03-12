package dev.wuliclaw.automaillogin.model;

import java.util.UUID;

public final class PlayerAccount {
    private final UUID uniqueId;
    private final String playerName;
    private String email;
    private String passwordHash;
    private String lastIp;
    private boolean secondFactorVerified;

    public PlayerAccount(UUID uniqueId, String playerName) {
        this.uniqueId = uniqueId;
        this.playerName = playerName;
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
    public boolean hasPassword() { return passwordHash != null && !passwordHash.isBlank(); }
}
