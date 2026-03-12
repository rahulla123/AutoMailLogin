package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.storage.AuditLogEntry;
import dev.wuliclaw.automaillogin.storage.AbstractSqlStorageProvider;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuditLogService {
    private final AbstractSqlStorageProvider storageProvider;

    public AuditLogService(AbstractSqlStorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public void log(Player player, String action, String detail) {
        String ip = player.getAddress() == null ? null : player.getAddress().getAddress().getHostAddress();
        storageProvider.appendAuditLog(new AuditLogEntry(
                player.getUniqueId(),
                player.getName(),
                action,
                detail,
                ip,
                Instant.now()
        ));
    }

    public List<AuditLogEntry> getRecent(UUID uniqueId, int limit) {
        return storageProvider.findRecentAuditLogs(uniqueId, limit);
    }
}
