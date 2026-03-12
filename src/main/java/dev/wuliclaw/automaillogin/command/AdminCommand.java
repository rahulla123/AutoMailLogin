package dev.wuliclaw.automaillogin.command;

import dev.wuliclaw.automaillogin.service.AuthService;
import dev.wuliclaw.automaillogin.storage.AuditLogEntry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private final AuthService authService;
    private final GuiCommand guiCommand;

    public AdminCommand(AuthService authService, GuiCommand guiCommand) {
        this.authService = authService;
        this.guiCommand = guiCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && "menu".equalsIgnoreCase(args[0])) {
            return guiCommand.onCommand(sender, command, label, new String[0]);
        }
        if (!sender.hasPermission("automaillogin.admin")) {
            sender.sendMessage("§c你没有权限执行这个命令。");
            return true;
        }
        if (args.length < 3 || !"admin".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§e用法: /automaillogin admin <force2fa|status|logs|unbindmail|resetauth> <player>");
            return true;
        }
        String subcommand = args[1].toLowerCase();
        String targetName = args[2];
        Player target = Bukkit.getPlayerExact(targetName);
        switch (subcommand) {
            case "force2fa" -> {
                if (target == null) {
                    sender.sendMessage("§cforce2fa 需要目标在线。");
                    return true;
                }
                authService.forceSecondFactor(target);
                sender.sendMessage("§a已标记玩家下次登录触发二次验证：" + target.getName());
                return true;
            }
            case "status" -> {
                sender.sendMessage(authService.describeStatusByName(targetName));
                return true;
            }
            case "logs" -> {
                List<AuditLogEntry> entries = authService.getRecentLogsByName(targetName, 10);
                sender.sendMessage("§6[AutoMailLogin] §r最近日志：" + targetName);
                if (entries.isEmpty()) {
                    sender.sendMessage("§7暂无日志记录。");
                    return true;
                }
                for (AuditLogEntry entry : entries) {
                    sender.sendMessage("§7- [" + entry.action() + "] " + entry.detail() + " @ " + entry.createdAt());
                }
                return true;
            }
            case "unbindmail" -> {
                if (target == null) {
                    sender.sendMessage("§cunbindmail 需要目标在线。");
                    return true;
                }
                authService.unbindEmail(target);
                sender.sendMessage("§a已解绑玩家邮箱：" + target.getName());
                return true;
            }
            case "resetauth" -> {
                if (target == null) {
                    sender.sendMessage("§cresetauth 需要目标在线。");
                    return true;
                }
                authService.resetAuthState(target);
                sender.sendMessage("§a已重置玩家认证状态：" + target.getName());
                return true;
            }
            default -> {
                sender.sendMessage("§e用法: /automaillogin admin <force2fa|status|logs|unbindmail|resetauth> <player>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("admin", "menu");
        }
        if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            return List.of("force2fa", "status", "logs", "unbindmail", "resetauth");
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0])) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
            return players;
        }
        return Collections.emptyList();
    }
}
