package dev.wuliclaw.automaillogin.storage;

import dev.wuliclaw.automaillogin.model.PlayerAccount;

import java.util.Optional;
import java.util.UUID;

public interface StorageProvider {
    void initialize();
    Optional<PlayerAccount> findByUniqueId(UUID uniqueId);
    void save(PlayerAccount account);
}
