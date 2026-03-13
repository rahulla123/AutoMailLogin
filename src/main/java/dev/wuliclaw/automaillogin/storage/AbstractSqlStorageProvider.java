package dev.wuliclaw.automaillogin.storage;

import dev.wuliclaw.automaillogin.model.PlayerAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractSqlStorageProvider implements StorageProvider {
    protected abstract Connection getConnection() throws SQLException;
    protected abstract void initializeSchema(Connection connection) throws SQLException;

    @Override
    public void initialize() {
        try (Connection connection = getConnection()) {
            initializeSchema(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize SQL storage", exception);
        }
    }

    @Override
    public Optional<PlayerAccount> findByUniqueId(UUID uniqueId) {
        return findOne("SELECT * FROM aml_players WHERE unique_id = ?", uniqueId.toString());
    }

    @Override
    public Optional<PlayerAccount> findByPlayerName(String playerName) {
        return findOne("SELECT * FROM aml_players WHERE LOWER(player_name) = LOWER(?)", playerName);
    }

    private Optional<PlayerAccount> findOne(String sql, String value) {
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPlayer(resultSet));
            }
        } catch (SQLException exception) {
            onError("Failed to load player account", exception);
            return Optional.empty();
        }
    }

    @Override
    public boolean testConnection() {
        try (Connection ignored = getConnection()) {
            return true;
        } catch (SQLException exception) {
            onError("Failed to test SQL connection", exception);
            return false;
        }
    }

    @Override
    public void save(PlayerAccount account) {
        String sql = upsertPlayerSql();
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getUniqueId().toString());
            statement.setString(2, account.getPlayerName());
            statement.setString(3, account.getEmail());
            statement.setString(4, account.getPasswordHash());
            statement.setString(5, account.getLastIp());
            statement.setInt(6, account.isSecondFactorVerified() ? 1 : 0);
            statement.setString(7, toText(account.getRegisteredAt()));
            statement.setString(8, toText(account.getLastLoginAt()));
            statement.setInt(9, account.getFailedLoginAttempts());
            statement.setString(10, toText(account.getLockedUntil()));
            statement.setString(11, toText(account.getTrustedUntil()));
            statement.setString(12, toText(account.getLastCodeSentAt()));
            statement.setString(13, account.getPendingCode());
            statement.setString(14, account.getPendingEmail());
            statement.setString(15, account.getPendingPurpose());
            statement.setString(16, toText(account.getPendingExpiresAt()));
            statement.setInt(17, account.getPendingFailedAttempts());
            statement.setString(18, toText(account.getPendingLockedUntil()));
            statement.setInt(19, account.isEmailVerified() ? 1 : 0);
            bindUpsertTail(statement, account);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save player account", exception);
        }
    }

    public void appendAuditLog(AuditLogEntry entry) {
        String sql = "INSERT INTO aml_audit_logs(unique_id, player_name, action, detail, ip_address, created_at) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.uniqueId().toString());
            statement.setString(2, entry.playerName());
            statement.setString(3, entry.action());
            statement.setString(4, entry.detail());
            statement.setString(5, entry.ipAddress());
            statement.setString(6, entry.createdAt().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            onError("Failed to append audit log", exception);
        }
    }

    public List<AuditLogEntry> findRecentAuditLogs(UUID uniqueId, int limit) {
        String sql = "SELECT unique_id, player_name, action, detail, ip_address, created_at FROM aml_audit_logs WHERE unique_id = ? ORDER BY id DESC LIMIT ?";
        List<AuditLogEntry> entries = new ArrayList<>();
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uniqueId.toString());
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new AuditLogEntry(UUID.fromString(resultSet.getString("unique_id")), resultSet.getString("player_name"), resultSet.getString("action"), resultSet.getString("detail"), resultSet.getString("ip_address"), Instant.parse(resultSet.getString("created_at"))));
                }
            }
        } catch (SQLException exception) {
            onError("Failed to query audit logs", exception);
        }
        return entries;
    }

    protected PlayerAccount mapPlayer(ResultSet resultSet) throws SQLException {
        PlayerAccount account = new PlayerAccount(UUID.fromString(resultSet.getString("unique_id")), resultSet.getString("player_name"));
        account.setEmail(resultSet.getString("email"));
        account.setPasswordHash(resultSet.getString("password_hash"));
        account.setLastIp(resultSet.getString("last_ip"));
        account.setSecondFactorVerified(resultSet.getInt("second_factor_verified") == 1);
        account.setRegisteredAt(fromText(resultSet.getString("registered_at"), Instant.now()));
        account.setLastLoginAt(fromText(resultSet.getString("last_login_at"), null));
        account.setFailedLoginAttempts(resultSet.getInt("failed_login_attempts"));
        account.setLockedUntil(fromText(resultSet.getString("locked_until"), null));
        account.setTrustedUntil(fromText(resultSet.getString("trusted_until"), null));
        account.setLastCodeSentAt(fromText(resultSet.getString("last_code_sent_at"), null));
        account.setPendingCode(resultSet.getString("pending_code"));
        account.setPendingEmail(resultSet.getString("pending_email"));
        account.setPendingPurpose(resultSet.getString("pending_purpose"));
        account.setPendingExpiresAt(fromText(resultSet.getString("pending_expires_at"), null));
        account.setPendingFailedAttempts(resultSet.getInt("pending_failed_attempts"));
        account.setPendingLockedUntil(fromText(resultSet.getString("pending_locked_until"), null));
        account.setEmailVerified(resultSet.getInt("email_verified") == 1);
        return account;
    }

    protected Instant fromText(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    protected String toText(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    protected void applyAlterStatements(Connection connection, List<String> sqlStatements) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : sqlStatements) {
                try {
                    statement.executeUpdate(sql);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    protected void onError(String message, SQLException exception) {
    }

    protected abstract String upsertPlayerSql();
    protected abstract void bindUpsertTail(PreparedStatement statement, PlayerAccount account) throws SQLException;
}
