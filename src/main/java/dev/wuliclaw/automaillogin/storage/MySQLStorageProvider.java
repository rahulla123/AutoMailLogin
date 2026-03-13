package dev.wuliclaw.automaillogin.storage;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PlayerAccount;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class MySQLStorageProvider extends AbstractSqlStorageProvider {
    private final AutoMailLoginPlugin plugin;

    public MySQLStorageProvider(AutoMailLoginPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        String host = plugin.getConfig().getString("database.mysql.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "automaillogin");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "change_me");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    protected void initializeSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS aml_players (unique_id VARCHAR(36) PRIMARY KEY, player_name VARCHAR(64) NOT NULL, email VARCHAR(255), password_hash TEXT, last_ip VARCHAR(64), second_factor_verified TINYINT NOT NULL DEFAULT 0, registered_at VARCHAR(64), last_login_at VARCHAR(64), failed_login_attempts INT NOT NULL DEFAULT 0, locked_until VARCHAR(64), trusted_until VARCHAR(64), last_code_sent_at VARCHAR(64), pending_code VARCHAR(255), pending_email VARCHAR(255), pending_purpose VARCHAR(64), pending_expires_at VARCHAR(64), pending_failed_attempts INT NOT NULL DEFAULT 0, pending_locked_until VARCHAR(64), email_verified TINYINT NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS aml_audit_logs (id BIGINT PRIMARY KEY AUTO_INCREMENT, unique_id VARCHAR(36) NOT NULL, player_name VARCHAR(64) NOT NULL, action VARCHAR(64) NOT NULL, detail TEXT, ip_address VARCHAR(64), created_at VARCHAR(64) NOT NULL)");
        }
        applyAlterStatements(connection, List.of(
                "ALTER TABLE aml_players ADD COLUMN registered_at VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN last_login_at VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0",
                "ALTER TABLE aml_players ADD COLUMN locked_until VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN trusted_until VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN last_code_sent_at VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN pending_code VARCHAR(32)",
                "ALTER TABLE aml_players ADD COLUMN pending_email VARCHAR(255)",
                "ALTER TABLE aml_players ADD COLUMN pending_purpose VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN pending_expires_at VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN pending_failed_attempts INT NOT NULL DEFAULT 0",
                "ALTER TABLE aml_players ADD COLUMN pending_locked_until VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN email_verified TINYINT NOT NULL DEFAULT 0"
        ));
    }

    @Override
    protected String upsertPlayerSql() {
        return "INSERT INTO aml_players(unique_id, player_name, email, password_hash, last_ip, second_factor_verified, registered_at, last_login_at, failed_login_attempts, locked_until, trusted_until, last_code_sent_at, pending_code, pending_email, pending_purpose, pending_expires_at, pending_failed_attempts, pending_locked_until, email_verified) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name = ?, email = ?, password_hash = ?, last_ip = ?, second_factor_verified = ?, registered_at = ?, last_login_at = ?, failed_login_attempts = ?, locked_until = ?, trusted_until = ?, last_code_sent_at = ?, pending_code = ?, pending_email = ?, pending_purpose = ?, pending_expires_at = ?, pending_failed_attempts = ?, pending_locked_until = ?, email_verified = ?";
    }

    @Override
    protected void bindUpsertTail(PreparedStatement statement, PlayerAccount account) throws SQLException {
        statement.setString(18, account.getPlayerName());
        statement.setString(19, account.getEmail());
        statement.setString(20, account.getPasswordHash());
        statement.setString(21, account.getLastIp());
        statement.setInt(22, account.isSecondFactorVerified() ? 1 : 0);
        statement.setString(23, toText(account.getRegisteredAt()));
        statement.setString(24, toText(account.getLastLoginAt()));
        statement.setInt(25, account.getFailedLoginAttempts());
        statement.setString(26, toText(account.getLockedUntil()));
        statement.setString(27, toText(account.getTrustedUntil()));
        statement.setString(28, toText(account.getLastCodeSentAt()));
        statement.setString(29, account.getPendingCode());
        statement.setString(30, account.getPendingEmail());
        statement.setString(31, account.getPendingPurpose());
        statement.setString(32, toText(account.getPendingExpiresAt()));
        statement.setInt(33, account.getPendingFailedAttempts());
        statement.setString(34, toText(account.getPendingLockedUntil()));
        statement.setInt(35, account.isEmailVerified() ? 1 : 0);
    }

    @Override
    protected void onError(String message, SQLException exception) {
        plugin.getLogger().warning(message + ": " + exception.getMessage());
    }
}
