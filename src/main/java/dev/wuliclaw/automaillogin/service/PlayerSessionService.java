package dev.wuliclaw.automaillogin.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSessionService {
    private final Map<UUID, Boolean> authenticatedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, String> currentActions = new ConcurrentHashMap<>();

    public void markAuthenticated(UUID uniqueId) {
        authenticatedPlayers.put(uniqueId, true);
        currentActions.remove(uniqueId);
    }

    public void markUnauthenticated(UUID uniqueId) {
        authenticatedPlayers.put(uniqueId, false);
    }

    public boolean isAuthenticated(UUID uniqueId) {
        return authenticatedPlayers.getOrDefault(uniqueId, false);
    }

    public void setCurrentAction(UUID uniqueId, String action) {
        currentActions.put(uniqueId, action);
    }

    public String getCurrentAction(UUID uniqueId) {
        return currentActions.get(uniqueId);
    }

    public void clear(UUID uniqueId) {
        authenticatedPlayers.remove(uniqueId);
        currentActions.remove(uniqueId);
    }
}
