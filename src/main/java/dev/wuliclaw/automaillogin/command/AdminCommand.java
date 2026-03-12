package dev.wuliclaw.automaillogin.command;

import dev.wuliclaw.automaillogin.service.AuthService;
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

    public AdminCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("automaillogin.admin")) {
            sender.sendMessage("§c你没有权限执行这个命令。");
            return true;
        }
        if (args.length < 2 || !"admin".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§e用法: /automaillogin admin <force2fa|status> <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§c找不到该在线玩家。");
            return true;
        }
        String subcommand = args[0].equalsIgnoreCase("admin") && args.length >= 2 ? (args.length >= 2 ? args[1] : "") : "";
        if (args.length >= 3) {
            subcommand = args[1].toLowerCase();
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§c找不到该在线玩家。");
                return true;
            }
        }
        switch (subcommand) {
            case "force2fa" -> {
                authService.forceSecondFactor(target);
                sender.sendMessage("§a已标记玩家下次登录触发二次验证：" + target.getName());
                return true;
            }
            case "status" -> {
                sender.sendMessage(authService.describeStatus(target));
                return true;
            }
            default -> {
                sender.sendMessage("§e用法: /automaillogin admin <force2fa|status> <player>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("admin");
        }
        if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            return List.of("force2fa", "status");
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0])) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
            return players;
        }
        return Collections.emptyList();
    }
}
