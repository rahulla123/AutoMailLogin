package dev.wuliclaw.automaillogin.security;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHasher {
    private static final String BCRYPT_PREFIX = "$2";
    private static final int BCRYPT_ROUNDS = 12;

    public String hash(String value) {
        return BCrypt.hashpw(value, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    public boolean matches(String rawValue, String hashedValue) {
        if (hashedValue == null || hashedValue.isBlank()) {
            return false;
        }
        if (hashedValue.startsWith(BCRYPT_PREFIX)) {
            return BCrypt.checkpw(rawValue, hashedValue);
        }
        return legacySha256(rawValue).equals(hashedValue);
    }

    public boolean needsRehash(String hashedValue) {
        return hashedValue == null || hashedValue.isBlank() || !hashedValue.startsWith(BCRYPT_PREFIX);
    }

    private String legacySha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
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
