package dev.wuliclaw.automaillogin.support;

import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.storage.StorageProvider;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryStorageProvider implements StorageProvider {
    private final Map<UUID, PlayerAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
    }

    @Override
    public Optional<PlayerAccount> findByUniqueId(UUID uniqueId) {
        return Optional.ofNullable(accounts.get(uniqueId));
    }

    @Override
    public Optional<PlayerAccount> findByPlayerName(String playerName) {
        return accounts.values().stream()
                .filter(account -> account.getPlayerName().equalsIgnoreCase(playerName))
                .findFirst();
    }

    @Override
    public void save(PlayerAccount account) {
        accounts.put(account.getUniqueId(), account);
    }

    @Override
    public boolean testConnection() {
        return true;
    }
}
