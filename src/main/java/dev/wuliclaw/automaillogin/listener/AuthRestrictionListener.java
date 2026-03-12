package dev.wuliclaw.automaillogin.listener;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.gui.AuthMenuHolder;
import dev.wuliclaw.automaillogin.service.AuthService;
import dev.wuliclaw.automaillogin.service.PlayerSessionService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public final class AuthRestrictionListener implements Listener {
    private final AutoMailLoginPlugin plugin;
    private final PlayerSessionService sessionService;
    private final AuthService authService;

    public AuthRestrictionListener(AutoMailLoginPlugin plugin, PlayerSessionService sessionService, AuthService authService) {
        this.plugin = plugin;
        this.sessionService = sessionService;
        this.authService = authService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        authService.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        authService.handleQuit(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (plugin.getConfig().getBoolean("restriction.block-move", true) && isBlocked(event.getPlayer()) && movedBlock(event)) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (plugin.getConfig().getBoolean("restriction.block-chat", true) && isBlocked(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("请先登录。"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isBlocked(event.getPlayer())) {
            return;
        }
        String input = event.getMessage().toLowerCase();
        List<String> allowedCommands = plugin.getConfig().getStringList("restriction.allowed-commands");
        boolean allowed = allowedCommands.stream().anyMatch(input::startsWith) || input.startsWith("/automaillogin menu");
        if (!allowed) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c请先完成登录验证。输入 /automaillogin menu 打开认证菜单。");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.getConfig().getBoolean("restriction.block-interact", true) && isBlocked(event.getPlayer()) && event.getAction() != Action.PHYSICAL) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof AuthMenuHolder) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getConfig().getBoolean("restriction.block-inventory", true)
                && event.getWhoClicked() instanceof Player player
                && isBlocked(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getConfig().getBoolean("restriction.block-drop", true) && isBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (plugin.getConfig().getBoolean("restriction.block-pickup", true)
                && event.getEntity() instanceof Player player
                && isBlocked(player)) {
            event.setCancelled(true);
        }
    }

    private boolean isBlocked(Player player) {
        return !sessionService.isAuthenticated(player.getUniqueId());
    }

    private boolean movedBlock(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return false;
        }
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }
}
