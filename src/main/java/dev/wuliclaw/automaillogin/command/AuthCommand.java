package dev.wuliclaw.automaillogin.command;

import dev.wuliclaw.automaillogin.service.AuthService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class AuthCommand implements CommandExecutor, TabCompleter {
    private final AuthService authService;

    public AuthCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "mailregister" -> {
                if (args.length != 1) {
                    player.sendMessage("用法: /mailregister <邮箱>");
                    return true;
                }
                authService.registerMail(player, args[0]);
                return true;
            }
            case "mailcode" -> {
                if (args.length != 1) {
                    player.sendMessage("用法: /mailcode <验证码>");
                    return true;
                }
                authService.verifyMail(player, args[0]);
                return true;
            }
            case "setpassword" -> {
                if (args.length != 2) {
                    player.sendMessage("用法: /setpassword <密码> <确认密码>");
                    return true;
                }
                authService.setPassword(player, args[0], args[1]);
                return true;
            }
            case "login" -> {
                if (args.length != 1) {
                    player.sendMessage("用法: /login <密码>");
                    return true;
                }
                authService.login(player, args[0]);
                return true;
            }
            case "forgotpassword" -> {
                if (args.length != 1) {
                    player.sendMessage("用法: /forgotpassword <邮箱>");
                    return true;
                }
                authService.forgotPassword(player, args[0]);
                return true;
            }
            case "resetpassword" -> {
                if (args.length != 3) {
                    player.sendMessage("用法: /resetpassword <验证码> <新密码> <确认密码>");
                    return true;
                }
                authService.resetPassword(player, args[0], args[1], args[2]);
                return true;
            }
            case "mail2fa" -> {
                if (args.length != 1) {
                    player.sendMessage("用法: /mail2fa <验证码>");
                    return true;
                }
                authService.verifySecondFactor(player, args[0]);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
