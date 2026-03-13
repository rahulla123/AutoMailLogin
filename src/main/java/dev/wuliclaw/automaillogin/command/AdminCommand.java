package dev.wuliclaw.automaillogin.command;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.service.AuthService;
import dev.wuliclaw.automaillogin.service.MessageService;
import dev.wuliclaw.automaillogin.storage.AuditLogEntry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private final AutoMailLoginPlugin plugin;
    private final AuthService authService;
    private final GuiCommand guiCommand;
    private final MessageService messageService;

    public AdminCommand(AutoMailLoginPlugin plugin, AuthService authService, GuiCommand guiCommand, MessageService messageService) {
        this.plugin = plugin;
        this.authService = authService;
        this.guiCommand = guiCommand;
        this.messageService = messageService;
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
        if (args.length >= 2 && "admin".equalsIgnoreCase(args[0]) && "reload".equalsIgnoreCase(args[1])) {
            plugin.reloadRuntime();
            sender.sendMessage("§aAutoMailLogin 配置、消息和邮件模板已重载。");
            return true;
        }
        if (args.length >= 3 && "admin".equalsIgnoreCase(args[0]) && "testsmtp".equalsIgnoreCase(args[1])) {
            boolean success = authService.sendTestMail(args[2]);
            sender.sendMessage(success ? "§a测试邮件发送请求已完成，请检查邮箱或服务端日志。" : "§c测试邮件发送失败，请检查邮箱格式、SMTP 配置或服务端日志。");
            return true;
        }
        if (args.length < 3 || !"admin".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§e用法: /automaillogin admin <reload|force2fa|status|logs|unbindmail|resetauth|testsmtp> <player|email>");
            return true;
        }
        String subcommand = args[1].toLowerCase();
        String targetName = args[2];
        switch (subcommand) {
            case "force2fa" -> {
                if (!authService.forceSecondFactorByName(targetName)) {
                    sender.sendMessage("§c找不到该玩家或玩家尚未注册。");
                    return true;
                }
                sender.sendMessage("§a已标记玩家下次登录触发二次验证：" + targetName);
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
                    sender.sendMessage("§7- [" + entry.action() + "] " + authService.maskAuditDetail(entry.detail()) + " @ " + entry.createdAt());
                }
                return true;
            }
            case "unbindmail" -> {
                if (!authService.unbindEmailByName(targetName)) {
                    sender.sendMessage("§c找不到该玩家或玩家尚未注册。");
                    return true;
                }
                sender.sendMessage("§a已解绑玩家邮箱：" + targetName);
                return true;
            }
            case "resetauth" -> {
                if (!authService.resetAuthStateByName(targetName)) {
                    sender.sendMessage("§c找不到该玩家或玩家尚未注册。");
                    return true;
                }
                sender.sendMessage("§a已重置玩家认证状态：" + targetName);
                return true;
            }
            default -> {
                sender.sendMessage("§e用法: /automaillogin admin <reload|force2fa|status|logs|unbindmail|resetauth|testsmtp> <player|email>");
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
            return List.of("reload", "force2fa", "status", "logs", "unbindmail", "resetauth", "testsmtp");
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && !"testsmtp".equalsIgnoreCase(args[1]) && !"reload".equalsIgnoreCase(args[1])) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
            return players;
        }
        return Collections.emptyList();
    }
}
