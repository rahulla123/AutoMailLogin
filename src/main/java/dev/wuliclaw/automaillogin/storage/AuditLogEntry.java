package dev.wuliclaw.automaillogin.storage;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEntry(
        UUID uniqueId,
        String playerName,
        String action,
        String detail,
        String ipAddress,
        Instant createdAt
) {
}
