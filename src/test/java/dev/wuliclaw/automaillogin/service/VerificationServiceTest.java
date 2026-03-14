package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PendingVerification;
import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import dev.wuliclaw.automaillogin.support.InMemoryStorageProvider;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerificationServiceTest {
    private final YamlConfiguration config = new YamlConfiguration();
    private final InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        config.set("mail.code-length", 6);
        config.set("mail.code-expire-seconds", 300);
        config.set("mail.resend-cooldown-seconds", 60);
        config.set("mail.resend-cooldown-register-seconds", 60);
        config.set("mail.resend-cooldown-reset-seconds", 120);
        config.set("mail.resend-cooldown-second-factor-seconds", 60);
        config.set("mail.max-verify-attempts", 5);
        config.set("mail.max-verify-attempts-register", 2);
        config.set("mail.max-verify-attempts-reset", 4);
        config.set("mail.max-verify-attempts-second-factor", 5);
        config.set("mail.verify-lock-seconds", 300);
        config.set("mail.verify-lock-register-seconds", 180);
        config.set("mail.verify-lock-reset-seconds", 600);
        config.set("mail.verify-lock-second-factor-seconds", 300);

        AutoMailLoginPlugin plugin = mock(AutoMailLoginPlugin.class);
        when(plugin.getConfig()).thenReturn(config);

        verificationService = new VerificationService(plugin, storageProvider);
    }

    @Test
    void createAndVerifySuccessClearsPendingState() {
        UUID uniqueId = UUID.randomUUID();
        PlayerAccount account = new PlayerAccount(uniqueId, "Steve");
        storageProvider.save(account);

        PendingVerification pendingVerification = verificationService.create(uniqueId, "steve@example.com", VerificationPurpose.REGISTER);

        PlayerAccount stored = storageProvider.findByUniqueId(uniqueId).orElseThrow();
        assertEquals("steve@example.com", stored.getPendingEmail());
        assertEquals(VerificationPurpose.REGISTER.name(), stored.getPendingPurpose());
        assertNotNull(stored.getPendingCode());
        assertNotEquals(pendingVerification.code(), stored.getPendingCode());

        boolean verified = verificationService.verify(uniqueId, pendingVerification.code(), VerificationPurpose.REGISTER);

        assertTrue(verified);
        PlayerAccount afterVerify = storageProvider.findByUniqueId(uniqueId).orElseThrow();
        assertNull(afterVerify.getPendingCode());
        assertNull(afterVerify.getPendingEmail());
        assertNull(afterVerify.getPendingPurpose());
        assertNull(afterVerify.getPendingExpiresAt());
    }

    @Test
    void verifyWrongCodeTriggersLockAfterConfiguredAttempts() {
        UUID uniqueId = UUID.randomUUID();
        PlayerAccount account = new PlayerAccount(uniqueId, "Alex");
        storageProvider.save(account);
        verificationService.create(uniqueId, "alex@example.com", VerificationPurpose.REGISTER);

        assertFalse(verificationService.verify(uniqueId, "111111", VerificationPurpose.REGISTER));
        assertEquals(1, storageProvider.findByUniqueId(uniqueId).orElseThrow().getPendingFailedAttempts());

        assertFalse(verificationService.verify(uniqueId, "222222", VerificationPurpose.REGISTER));
        PlayerAccount locked = storageProvider.findByUniqueId(uniqueId).orElseThrow();
        assertEquals(0, locked.getPendingFailedAttempts());
        assertTrue(locked.isPendingLocked());
        assertTrue(verificationService.getRemainingVerifyLockSeconds(uniqueId) > 0);
    }

    @Test
    void canSendRespectsCooldownWindow() {
        UUID uniqueId = UUID.randomUUID();
        PlayerAccount account = new PlayerAccount(uniqueId, "Herobrine");
        account.setLastCodeSentAt(Instant.now());
        storageProvider.save(account);

        assertFalse(verificationService.canSend(uniqueId, VerificationPurpose.RESET_PASSWORD));
        assertTrue(verificationService.getRemainingCooldownSeconds(uniqueId, VerificationPurpose.RESET_PASSWORD) > 0);

        account.setLastCodeSentAt(Instant.now().minusSeconds(180));
        storageProvider.save(account);

        assertTrue(verificationService.canSend(uniqueId, VerificationPurpose.RESET_PASSWORD));
        assertEquals(0, verificationService.getRemainingCooldownSeconds(uniqueId, VerificationPurpose.RESET_PASSWORD));
    }
}
