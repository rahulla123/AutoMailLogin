package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import dev.wuliclaw.automaillogin.security.SecondFactorMode;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SecondFactorService {
    private final AutoMailLoginPlugin plugin;
    private final VerificationService verificationService;
    private final Map<UUID, Boolean> pendingSecondFactor = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> forceAfterReset = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> forceByAdmin = new ConcurrentHashMap<>();

    public SecondFactorService(AutoMailLoginPlugin plugin, VerificationService verificationService) {
        this.plugin = plugin;
        this.verificationService = verificationService;
    }

    public boolean shouldRequireSecondFactor(Player player, PlayerAccount account) {
        boolean enabled = plugin.getConfig().getBoolean("security.second-factor.enabled", true);
        if (!enabled) {
            return false;
        }
        SecondFactorMode mode = SecondFactorMode.fromConfig(plugin.getConfig().getString("security.second-factor.mode", "on_new_ip"));
        String currentIp = player.getAddress() == null ? null : player.getAddress().getAddress().getHostAddress();
        return switch (mode) {
            case DISABLED -> false;
            case ALWAYS -> true;
            case ON_NEW_IP -> currentIp != null && account.getLastIp() != null && !currentIp.equals(account.getLastIp());
            case ON_PASSWORD_RESET -> forceAfterReset.getOrDefault(player.getUniqueId(), false);
            case ON_ADMIN_FORCE -> forceByAdmin.getOrDefault(player.getUniqueId(), false);
            case MIXED -> forceAfterReset.getOrDefault(player.getUniqueId(), false)
                    || forceByAdmin.getOrDefault(player.getUniqueId(), false)
                    || (currentIp != null && account.getLastIp() != null && !currentIp.equals(account.getLastIp()));
        };
    }

    public void begin(Player player, String email, MailService mailService) {
        pendingSecondFactor.put(player.getUniqueId(), true);
        mailService.sendSecondFactorCode(player, email);
    }

    public boolean verify(Player player, String code) {
        boolean success = verificationService.verify(player.getUniqueId(), code, VerificationPurpose.SECOND_FACTOR);
        if (success) {
            pendingSecondFactor.remove(player.getUniqueId());
            forceAfterReset.remove(player.getUniqueId());
            forceByAdmin.remove(player.getUniqueId());
        }
        return success;
    }

    public boolean isPending(Player player) {
        return pendingSecondFactor.getOrDefault(player.getUniqueId(), false);
    }

    public void markPasswordReset(UUID uniqueId) {
        forceAfterReset.put(uniqueId, true);
    }

    public void markAdminForce(UUID uniqueId) {
        forceByAdmin.put(uniqueId, true);
    }

    public void clear(UUID uniqueId) {
        pendingSecondFactor.remove(uniqueId);
        forceAfterReset.remove(uniqueId);
        forceByAdmin.remove(uniqueId);
    }
}
