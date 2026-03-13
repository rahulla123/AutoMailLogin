package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class MessageService {
    private final FileConfiguration messages;

    public MessageService(AutoMailLoginPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key, String fallback) {
        return messages.getString(key, fallback);
    }

    public String getPrefixed(String key, String fallback) {
        String prefix = get("prefix", "§6[AutoMailLogin] §r");
        return prefix + get(key, fallback);
    }
}
