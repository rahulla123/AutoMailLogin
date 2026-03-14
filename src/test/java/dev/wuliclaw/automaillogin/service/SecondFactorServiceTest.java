package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PendingVerification;
import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import dev.wuliclaw.automaillogin.support.InMemoryStorageProvider;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecondFactorServiceTest {
    private final YamlConfiguration config = new YamlConfiguration();
    private final InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();
    private VerificationService verificationService;
    private SecondFactorService secondFactorService;

    @BeforeEach
    void setUp() {
        config.set("mail.code-length", 6);
        config.set("mail.code-expire-seconds", 300);
        config.set("mail.max-verify-attempts-second-factor", 5);
        config.set("mail.verify-lock-second-factor-seconds", 300);
        config.set("security.second-factor.enabled", true);
        config.set("security.second-factor.mode", "on_new_ip");
        config.set("security.second-factor.trusted-days", 30);

        AutoMailLoginPlugin plugin = mock(AutoMailLoginPlugin.class);
        when(plugin.getConfig()).thenReturn(config);

        verificationService = new VerificationService(plugin, storageProvider);
        secondFactorService = new SecondFactorService(plugin, verificationService, storageProvider);
    }

    @Test
    void shouldRequireSecondFactorOnNewIp() {
        UUID uniqueId = UUID.randomUUID();
        PlayerAccount account = new PlayerAccount(uniqueId, "Steve");
        account.setLastIp("10.0.0.1");
        storageProvider.save(account);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uniqueId);
        when(player.getAddress()).thenReturn(new InetSocketAddress("10.0.0.2", 25565));

        assertTrue(secondFactorService.shouldRequireSecondFactor(player, account));
    }

    @Test
    void shouldSkipSecondFactorForTrustedAccount() {
        UUID uniqueId = UUID.randomUUID();
        PlayerAccount account = new PlayerAccount(uniqueId, "Alex");
        account.setLastIp("10.0.0.1");
        account.setTrustedUntil(Instant.now().plusSeconds(3600));
        storageProvider.save(account);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uniqueId);
        when(player.getAddress()).thenReturn(new InetSocketAddress("10.0.0.2", 25565));

        assertFalse(secondFactorService.shouldRequireSecondFactor(player, account));
    }

    @Test
    void beginAndVerifyMarksTrustedDeviceAndClearsPendingState() {
        UUID uniqueId = UUID.randomUUID();
        PlayerAccount account = new PlayerAccount(uniqueId, "Notch");
        storageProvider.save(account);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uniqueId);
        when(player.getAddress()).thenReturn(new InetSocketAddress("10.0.0.3", 25565));

        MailService mailService = mock(MailService.class);
        secondFactorService.begin(player, "notch@example.com", mailService);
        verify(mailService).sendSecondFactorCode(player, "notch@example.com");
        assertTrue(secondFactorService.isPending(player));

        PendingVerification verification = verificationService.create(uniqueId, "notch@example.com", VerificationPurpose.SECOND_FACTOR);
        assertTrue(secondFactorService.verify(player, verification.code()));

        PlayerAccount updated = storageProvider.findByUniqueId(uniqueId).orElseThrow();
        assertNotNull(updated.getTrustedUntil());
        assertTrue(updated.getTrustedUntil().isAfter(Instant.now()));
        assertFalse(secondFactorService.isPending(player));
    }
}
