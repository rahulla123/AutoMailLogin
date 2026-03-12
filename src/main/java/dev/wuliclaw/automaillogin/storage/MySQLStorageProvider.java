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
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS aml_players (unique_id VARCHAR(36) PRIMARY KEY, player_name VARCHAR(64) NOT NULL, email VARCHAR(255), password_hash TEXT, last_ip VARCHAR(64), second_factor_verified TINYINT NOT NULL DEFAULT 0, registered_at VARCHAR(64), last_login_at VARCHAR(64), failed_login_attempts INT NOT NULL DEFAULT 0, locked_until VARCHAR(64), trusted_until VARCHAR(64), last_code_sent_at VARCHAR(64))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS aml_audit_logs (id BIGINT PRIMARY KEY AUTO_INCREMENT, unique_id VARCHAR(36) NOT NULL, player_name VARCHAR(64) NOT NULL, action VARCHAR(64) NOT NULL, detail TEXT, ip_address VARCHAR(64), created_at VARCHAR(64) NOT NULL)");
        }
        applyAlterStatements(connection, List.of(
                "ALTER TABLE aml_players ADD COLUMN registered_at VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN last_login_at VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0",
                "ALTER TABLE aml_players ADD COLUMN locked_until VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN trusted_until VARCHAR(64)",
                "ALTER TABLE aml_players ADD COLUMN last_code_sent_at VARCHAR(64)"
        ));
    }

    @Override
    protected String upsertPlayerSql() {
        return "INSERT INTO aml_players(unique_id, player_name, email, password_hash, last_ip, second_factor_verified, registered_at, last_login_at, failed_login_attempts, locked_until, trusted_until, last_code_sent_at) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name = ?, email = ?, password_hash = ?, last_ip = ?, second_factor_verified = ?, registered_at = ?, last_login_at = ?, failed_login_attempts = ?, locked_until = ?, trusted_until = ?, last_code_sent_at = ?";
    }

    @Override
    protected void bindUpsertTail(PreparedStatement statement, PlayerAccount account) throws SQLException {
        statement.setString(13, account.getPlayerName());
        statement.setString(14, account.getEmail());
        statement.setString(15, account.getPasswordHash());
        statement.setString(16, account.getLastIp());
        statement.setInt(17, account.isSecondFactorVerified() ? 1 : 0);
        statement.setString(18, toText(account.getRegisteredAt()));
        statement.setString(19, toText(account.getLastLoginAt()));
        statement.setInt(20, account.getFailedLoginAttempts());
        statement.setString(21, toText(account.getLockedUntil()));
        statement.setString(22, toText(account.getTrustedUntil()));
        statement.setString(23, toText(account.getLastCodeSentAt()));
    }

    @Override
    protected void onError(String message, SQLException exception) {
        plugin.getLogger().warning(message + ": " + exception.getMessage());
    }
}
