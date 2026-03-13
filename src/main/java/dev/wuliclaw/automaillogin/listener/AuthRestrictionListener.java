package dev.wuliclaw.automaillogin.listener;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.gui.AuthMenuHolder;
import dev.wuliclaw.automaillogin.service.AuthService;
import dev.wuliclaw.automaillogin.service.MessageService;
import dev.wuliclaw.automaillogin.service.PlayerSessionService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
    private final MessageService messageService;

    public AuthRestrictionListener(AutoMailLoginPlugin plugin, PlayerSessionService sessionService, AuthService authService, MessageService messageService) {
        this.plugin = plugin;
        this.sessionService = sessionService;
        this.authService = authService;
        this.messageService = messageService;
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
        Player player = event.getPlayer();
        String currentAction = sessionService.getCurrentAction(player.getUniqueId());
        if (currentAction != null && !currentAction.isBlank()) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            plugin.getServer().getScheduler().runTask(plugin, () -> handleGuiInput(player, currentAction, message));
            return;
        }
        if (plugin.getConfig().getBoolean("restriction.block-chat", true) && isBlocked(player)) {
            event.setCancelled(true);
            player.sendMessage(Component.text(messageService.get("need-auth", "请先完成邮箱绑定或登录。")));
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
            if (!(event.getWhoClicked() instanceof Player player) || event.getCurrentItem() == null) {
                return;
            }
            switch (event.getSlot()) {
                case 10 -> {
                    sessionService.setCurrentAction(player.getUniqueId(), "mailregister");
                    player.closeInventory();
                    player.sendMessage("§a请输入邮箱地址。");
                }
                case 11 -> {
                    sessionService.setCurrentAction(player.getUniqueId(), "mailcode");
                    player.closeInventory();
                    player.sendMessage("§a请输入收到的验证码。");
                }
                case 12 -> {
                    sessionService.setCurrentAction(player.getUniqueId(), "setpassword");
                    player.closeInventory();
                    player.sendMessage("§a请输入：密码 空格 确认密码");
                }
                case 14 -> {
                    sessionService.setCurrentAction(player.getUniqueId(), "login");
                    player.closeInventory();
                    player.sendMessage("§a请输入登录密码。");
                }
                case 15 -> {
                    sessionService.setCurrentAction(player.getUniqueId(), "mail2fa");
                    player.closeInventory();
                    player.sendMessage("§a请输入二次验证验证码。");
                }
                case 16 -> {
                    sessionService.setCurrentAction(player.getUniqueId(), "forgotpassword");
                    player.closeInventory();
                    player.sendMessage("§a请输入邮箱地址以发送重置验证码。发送后会继续提示你输入：验证码 空格 新密码 空格 确认密码");
                }
                case 26 -> {
                    sessionService.setCurrentAction(player.getUniqueId(), null);
                    player.closeInventory();
                    player.sendMessage("§e已取消当前输入流程。");
                }
                default -> {
                }
            }
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

    private void handleGuiInput(Player player, String action, String message) {
        if ("cancel".equalsIgnoreCase(message)) {
            sessionService.setCurrentAction(player.getUniqueId(), null);
            player.sendMessage("§e已取消当前输入流程。");
            return;
        }
        switch (action) {
            case "mailregister" -> {
                sessionService.setCurrentAction(player.getUniqueId(), null);
                authService.registerMail(player, message);
            }
            case "mailcode" -> {
                sessionService.setCurrentAction(player.getUniqueId(), null);
                authService.verifyMail(player, message);
            }
            case "login" -> {
                sessionService.setCurrentAction(player.getUniqueId(), null);
                authService.login(player, message);
            }
            case "mail2fa" -> {
                sessionService.setCurrentAction(player.getUniqueId(), null);
                authService.verifySecondFactor(player, message);
            }
            case "forgotpassword" -> {
                authService.forgotPassword(player, message);
                sessionService.setCurrentAction(player.getUniqueId(), "resetpassword");
                player.sendMessage("§a请输入：验证码 空格 新密码 空格 确认密码");
            }
            case "setpassword" -> {
                sessionService.setCurrentAction(player.getUniqueId(), null);
                String[] parts = message.split("\\s+", 2);
                if (parts.length < 2) {
                    player.sendMessage("§c格式错误，请输入：密码 空格 确认密码");
                    return;
                }
                authService.setPassword(player, parts[0], parts[1]);
            }
            case "resetpassword" -> {
                sessionService.setCurrentAction(player.getUniqueId(), null);
                String[] parts = message.split("\\s+", 3);
                if (parts.length < 3) {
                    player.sendMessage("§c格式错误，请输入：验证码 空格 新密码 空格 确认密码");
                    return;
                }
                authService.resetPassword(player, parts[0], parts[1], parts[2]);
            }
            default -> {
                sessionService.setCurrentAction(player.getUniqueId(), null);
                player.sendMessage("§c未知的 GUI 操作，请重新打开菜单。");
            }
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
