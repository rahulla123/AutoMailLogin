package dev.wuliclaw.automaillogin.storage;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PlayerAccount;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class SQLiteStorageProvider extends AbstractSqlStorageProvider {
    private final AutoMailLoginPlugin plugin;
    private final File databaseFile;

    public SQLiteStorageProvider(AutoMailLoginPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.sqlite-file", "data.db"));
    }

    @Override
    protected Connection getConnection() throws SQLException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Failed to create plugin data folder");
        }
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }

    @Override
    protected void initializeSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS aml_players (unique_id TEXT PRIMARY KEY, player_name TEXT NOT NULL, email TEXT, password_hash TEXT, last_ip TEXT, second_factor_verified INTEGER NOT NULL DEFAULT 0, registered_at TEXT, last_login_at TEXT, failed_login_attempts INTEGER NOT NULL DEFAULT 0, locked_until TEXT, trusted_until TEXT, last_code_sent_at TEXT, pending_code TEXT, pending_email TEXT, pending_purpose TEXT, pending_expires_at TEXT, pending_failed_attempts INTEGER NOT NULL DEFAULT 0, pending_locked_until TEXT, email_verified INTEGER NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS aml_audit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, unique_id TEXT NOT NULL, player_name TEXT NOT NULL, action TEXT NOT NULL, detail TEXT, ip_address TEXT, created_at TEXT NOT NULL)");
        }
        applyAlterStatements(connection, List.of(
                "ALTER TABLE aml_players ADD COLUMN registered_at TEXT",
                "ALTER TABLE aml_players ADD COLUMN last_login_at TEXT",
                "ALTER TABLE aml_players ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE aml_players ADD COLUMN locked_until TEXT",
                "ALTER TABLE aml_players ADD COLUMN trusted_until TEXT",
                "ALTER TABLE aml_players ADD COLUMN last_code_sent_at TEXT",
                "ALTER TABLE aml_players ADD COLUMN pending_code TEXT",
                "ALTER TABLE aml_players ADD COLUMN pending_email TEXT",
                "ALTER TABLE aml_players ADD COLUMN pending_purpose TEXT",
                "ALTER TABLE aml_players ADD COLUMN pending_expires_at TEXT",
                "ALTER TABLE aml_players ADD COLUMN pending_failed_attempts INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE aml_players ADD COLUMN pending_locked_until TEXT",
                "ALTER TABLE aml_players ADD COLUMN email_verified INTEGER NOT NULL DEFAULT 0"
        ));
    }

    @Override
    protected String upsertPlayerSql() {
        return "INSERT INTO aml_players(unique_id, player_name, email, password_hash, last_ip, second_factor_verified, registered_at, last_login_at, failed_login_attempts, locked_until, trusted_until, last_code_sent_at, pending_code, pending_email, pending_purpose, pending_expires_at, pending_failed_attempts, pending_locked_until, email_verified) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(unique_id) DO UPDATE SET player_name = excluded.player_name, email = excluded.email, password_hash = excluded.password_hash, last_ip = excluded.last_ip, second_factor_verified = excluded.second_factor_verified, registered_at = excluded.registered_at, last_login_at = excluded.last_login_at, failed_login_attempts = excluded.failed_login_attempts, locked_until = excluded.locked_until, trusted_until = excluded.trusted_until, last_code_sent_at = excluded.last_code_sent_at, pending_code = excluded.pending_code, pending_email = excluded.pending_email, pending_purpose = excluded.pending_purpose, pending_expires_at = excluded.pending_expires_at, pending_failed_attempts = excluded.pending_failed_attempts, pending_locked_until = excluded.pending_locked_until, email_verified = excluded.email_verified";
    }

    @Override
    protected void bindUpsertTail(PreparedStatement statement, PlayerAccount account) {
    }

    @Override
    protected void onError(String message, SQLException exception) {
        plugin.getLogger().warning(message + ": " + exception.getMessage());
    }
}
