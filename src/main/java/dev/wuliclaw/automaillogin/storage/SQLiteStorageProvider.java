package dev.wuliclaw.automaillogin.storage;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PlayerAccount;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public final class SQLiteStorageProvider implements StorageProvider {
    private final AutoMailLoginPlugin plugin;
    private final File databaseFile;

    public SQLiteStorageProvider(AutoMailLoginPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.sqlite-file", "data.db"));
    }

    @Override
    public void initialize() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Failed to create plugin data folder");
        }
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS aml_players (
                        unique_id TEXT PRIMARY KEY,
                        player_name TEXT NOT NULL,
                        email TEXT,
                        password_hash TEXT,
                        last_ip TEXT,
                        second_factor_verified INTEGER NOT NULL DEFAULT 0
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize SQLite storage", exception);
        }
    }

    @Override
    public Optional<PlayerAccount> findByUniqueId(UUID uniqueId) {
        String sql = "SELECT unique_id, player_name, email, password_hash, last_ip, second_factor_verified FROM aml_players WHERE unique_id = ?";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uniqueId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                PlayerAccount account = new PlayerAccount(
                        UUID.fromString(resultSet.getString("unique_id")),
                        resultSet.getString("player_name")
                );
                account.setEmail(resultSet.getString("email"));
                account.setPasswordHash(resultSet.getString("password_hash"));
                account.setLastIp(resultSet.getString("last_ip"));
                account.setSecondFactorVerified(resultSet.getInt("second_factor_verified") == 1);
                return Optional.of(account);
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Failed to load player account: " + exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(PlayerAccount account) {
        String sql = """
                INSERT INTO aml_players(unique_id, player_name, email, password_hash, last_ip, second_factor_verified)
                VALUES(?, ?, ?, ?, ?, ?)
                ON CONFLICT(unique_id) DO UPDATE SET
                    player_name = excluded.player_name,
                    email = excluded.email,
                    password_hash = excluded.password_hash,
                    last_ip = excluded.last_ip,
                    second_factor_verified = excluded.second_factor_verified
                """;
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getUniqueId().toString());
            statement.setString(2, account.getPlayerName());
            statement.setString(3, account.getEmail());
            statement.setString(4, account.getPasswordHash());
            statement.setString(5, account.getLastIp());
            statement.setInt(6, account.isSecondFactorVerified() ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save player account", exception);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }
}
