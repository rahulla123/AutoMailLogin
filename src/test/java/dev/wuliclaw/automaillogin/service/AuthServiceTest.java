package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import dev.wuliclaw.automaillogin.security.PasswordHasher;
import dev.wuliclaw.automaillogin.support.InMemoryStorageProvider;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    @TempDir
    Path tempDir;

    private final YamlConfiguration config = new YamlConfiguration();
    private final InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
    private PlayerSessionService sessionService;
    private VerificationService verificationService;
    private MailService mailService;
    private PasswordHasher passwordHasher;
    private SecondFactorService secondFactorService;
    private AuditLogService auditLogService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        config.set("security.password-min-length", 8);
        config.set("security.max-login-attempts", 2);
        config.set("security.lock-seconds", 300);
        config.set("security.second-factor.enabled", true);
        config.set("security.second-factor.mode", "on_new_ip");
        config.set("security.second-factor.trusted-days", 30);
        config.set("mail.allowed-domains", java.util.List.of());
        config.set("mail.blocked-domains", java.util.List.of());

        AutoMailLoginPlugin plugin = mock(AutoMailLoginPlugin.class);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());

        sessionService = new PlayerSessionService();
        verificationService = mock(VerificationService.class);
        mailService = mock(MailService.class);
        passwordHasher = new PasswordHasher();
        secondFactorService = mock(SecondFactorService.class);
        auditLogService = mock(AuditLogService.class);

        authService = new AuthService(
                plugin,
                sessionService,
                verificationService,
                mailService,
                storageProvider,
                passwordHasher,
                secondFactorService,
                auditLogService
        );
    }

    @Test
    void registerMailStoresEmailAndRequestsVerificationCode() {
        Player player = mockPlayer("Steve");

        authService.registerMail(player, "steve@example.com");

        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElseThrow();
        assertEquals("steve@example.com", account.getEmail());
        assertFalse(account.isEmailVerified());
        verify(mailService).sendRegistrationCode(player, "steve@example.com");
    }

    @Test
    void setPasswordAuthenticatesVerifiedAccount() {
        Player player = mockPlayer("Alex");
        PlayerAccount account = new PlayerAccount(player.getUniqueId(), player.getName());
        account.setEmail("alex@example.com");
        account.setEmailVerified(true);
        storageProvider.save(account);

        authService.setPassword(player, "password123", "password123");

        PlayerAccount updated = storageProvider.findByUniqueId(player.getUniqueId()).orElseThrow();
        assertTrue(updated.hasPassword());
        assertTrue(passwordHasher.matches("password123", updated.getPasswordHash()));
        assertTrue(sessionService.isAuthenticated(player.getUniqueId()));
    }

    @Test
    void loginWrongPasswordLocksAfterConfiguredAttempts() {
        Player player = mockPlayer("Herobrine");
        PlayerAccount account = new PlayerAccount(player.getUniqueId(), player.getName());
        account.setEmail("hero@example.com");
        account.setEmailVerified(true);
        account.setPasswordHash(passwordHasher.hash("correct-password"));
        storageProvider.save(account);
        when(secondFactorService.shouldRequireSecondFactor(any(), any())).thenReturn(false);

        authService.login(player, "wrong-1");
        PlayerAccount afterFirst = storageProvider.findByUniqueId(player.getUniqueId()).orElseThrow();
        assertEquals(1, afterFirst.getFailedLoginAttempts());
        assertNull(afterFirst.getLockedUntil());

        authService.login(player, "wrong-2");
        PlayerAccount afterSecond = storageProvider.findByUniqueId(player.getUniqueId()).orElseThrow();
        assertEquals(0, afterSecond.getFailedLoginAttempts());
        assertNotNull(afterSecond.getLockedUntil());
        assertTrue(afterSecond.isLocked());
    }

    @Test
    void loginSuccessCompletesSessionWhenSecondFactorNotRequired() {
        Player player = mockPlayer("Notch");
        PlayerAccount account = new PlayerAccount(player.getUniqueId(), player.getName());
        account.setEmail("notch@example.com");
        account.setEmailVerified(true);
        account.setPasswordHash(passwordHasher.hash("correct-password"));
        storageProvider.save(account);
        when(secondFactorService.shouldRequireSecondFactor(any(), any())).thenReturn(false);

        authService.login(player, "correct-password");

        PlayerAccount updated = storageProvider.findByUniqueId(player.getUniqueId()).orElseThrow();
        assertTrue(sessionService.isAuthenticated(player.getUniqueId()));
        assertEquals("127.0.0.1", updated.getLastIp());
        assertTrue(updated.isSecondFactorVerified());
        assertNotNull(updated.getLastLoginAt());
        verify(secondFactorService, never()).begin(any(), any(), any());
    }

    @Test
    void resetPasswordUpdatesHashAndClearsTrustOnSuccessfulCode() {
        Player player = mockPlayer("Builder");
        PlayerAccount account = new PlayerAccount(player.getUniqueId(), player.getName());
        account.setEmail("builder@example.com");
        account.setEmailVerified(true);
        account.setPasswordHash(passwordHasher.hash("old-password"));
        account.setTrustedUntil(Instant.now().plusSeconds(3600));
        storageProvider.save(account);
        when(verificationService.verify(player.getUniqueId(), "654321", VerificationPurpose.RESET_PASSWORD)).thenReturn(true);

        authService.resetPassword(player, "654321", "new-password", "new-password");

        PlayerAccount updated = storageProvider.findByUniqueId(player.getUniqueId()).orElseThrow();
        assertTrue(passwordHasher.matches("new-password", updated.getPasswordHash()));
        assertNull(updated.getTrustedUntil());
        assertFalse(updated.isSecondFactorVerified());
        verify(secondFactorService).markPasswordReset(player.getUniqueId());
    }

    private Player mockPlayer(String name) {
        Player player = mock(Player.class);
        UUID uniqueId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uniqueId);
        when(player.getName()).thenReturn(name);
        when(player.getAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 25565));
        return player;
    }
}
