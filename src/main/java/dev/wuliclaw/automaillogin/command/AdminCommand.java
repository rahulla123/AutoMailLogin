package dev.wuliclaw.automaillogin.command;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.mail.MailTemplateType;
import dev.wuliclaw.automaillogin.service.AuthService;
import dev.wuliclaw.automaillogin.service.MessageService;
import dev.wuliclaw.automaillogin.storage.AuditLogEntry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
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
        if (args.length >= 2 && "admin".equalsIgnoreCase(args[0]) && "doctor".equalsIgnoreCase(args[1])) {
            runDoctor(sender);
            return true;
        }
        if (args.length >= 4 && "admin".equalsIgnoreCase(args[0]) && "previewmail".equalsIgnoreCase(args[1])) {
            MailTemplateType templateType = MailTemplateType.fromInput(args[2]);
            if (templateType == null) {
                sender.sendMessage(messageService.get("previewmail-invalid-type", "模板类型无效，可用值：register、reset-password、second-factor、test-smtp。"));
                return true;
            }
            if (!authService.sendTestMail(args[3])) {
                sender.sendMessage(messageService.get("previewmail-failed", "模板预览邮件发送失败，请检查邮箱格式、SMTP 配置或服务端日志。"));
                return true;
            }
            sender.sendMessage(messageService.get("previewmail-queued", "模板预览邮件正在异步发送，请稍后检查邮箱或服务端日志。"));
            authService.sendPreviewMailAsync(templateType, args[3], success -> sender.sendMessage(success
                    ? messageService.get("previewmail-sent", "模板预览邮件已发送，请检查邮箱或服务端日志。")
                    : messageService.get("previewmail-failed", "模板预览邮件发送失败，请检查邮箱格式、SMTP 配置或服务端日志。")));
            return true;
        }
        if (args.length >= 3 && "admin".equalsIgnoreCase(args[0]) && "testsmtp".equalsIgnoreCase(args[1])) {
            boolean valid = authService.sendTestMail(args[2]);
            if (!valid) {
                sender.sendMessage("§c测试邮件发送失败，请检查邮箱格式、SMTP 配置或服务端日志。");
                return true;
            }
            sender.sendMessage("§e测试邮件正在异步发送，请稍后检查邮箱或服务端日志。");
            authService.sendTestMailAsync(args[2], success -> sender.sendMessage(success ? "§a测试邮件发送请求已完成，请检查邮箱或服务端日志。" : "§c测试邮件发送失败，请检查邮箱格式、SMTP 配置或服务端日志。"));
            return true;
        }
        if (args.length < 3 || !"admin".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§e用法: /automaillogin admin <reload|doctor|previewmail|force2fa|status|logs|unbindmail|resetauth|testsmtp> <player|email>");
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
                sender.sendMessage("§e用法: /automaillogin admin <reload|doctor|previewmail|force2fa|status|logs|unbindmail|resetauth|testsmtp> <player|email>");
                return true;
            }
        }
    }

    private void runDoctor(CommandSender sender) {
        sender.sendMessage(messageService.get("doctor-title", "§6[AutoMailLogin] §r自检结果："));
        String databaseType = plugin.getConfig().getString("database.type", "sqlite");
        sender.sendMessage("§7- 数据库模式: §f" + databaseType);

        String mailMode = plugin.getConfig().getString("mail.mode", "mock");
        sender.sendMessage("§7- 邮件模式: §f" + mailMode);
        if ("smtp".equalsIgnoreCase(mailMode)) {
            String smtpHost = plugin.getConfig().getString("mail.smtp.host", "");
            int smtpPort = plugin.getConfig().getInt("mail.smtp.port", 0);
            boolean smtpUserSet = !plugin.getConfig().getString("mail.smtp.username", "").isBlank() && !"change_me".equals(plugin.getConfig().getString("mail.smtp.username", ""));
            boolean smtpPasswordSet = !plugin.getConfig().getString("mail.smtp.password", "").isBlank() && !"change_me".equals(plugin.getConfig().getString("mail.smtp.password", ""));
            boolean starttls = plugin.getConfig().getBoolean("mail.smtp.starttls", true);
            boolean requireStarttls = plugin.getConfig().getBoolean("mail.smtp.require-starttls", true);
            int timeoutMs = plugin.getConfig().getInt("mail.smtp.timeout-ms", 10000);
            sender.sendMessage("§7- SMTP 主机: " + (smtpHost.isBlank() ? "§c未配置" : "§a" + smtpHost + ":" + smtpPort));
            sender.sendMessage("§7- SMTP 账号: " + (smtpUserSet ? "§a已配置" : "§c未配置"));
            sender.sendMessage("§7- SMTP 密码: " + (smtpPasswordSet ? "§a已配置" : "§c未配置"));
            sender.sendMessage("§7- STARTTLS: " + (starttls ? "§a开启" : "§c关闭") + " / required=" + (requireStarttls ? "§a是" : "§e否"));
            sender.sendMessage("§7- SMTP 超时: §f" + timeoutMs + "ms");
        }

        File templateDir = plugin.getMailTemplateService().getTemplateDirectory();
        sender.sendMessage("§7- 模板目录: " + (templateDir.exists() ? "§a" + templateDir.getPath() : "§c缺失: " + templateDir.getPath()));
        for (MailTemplateType type : MailTemplateType.values()) {
            boolean subject = plugin.getMailTemplateService().hasTemplate(type, ".subject.txt");
            boolean text = plugin.getMailTemplateService().hasTemplate(type, ".text.txt");
            boolean html = plugin.getMailTemplateService().hasTemplate(type, ".html");
            sender.sendMessage("§7- 模板 " + type.filePrefix() + ": subject=" + flag(subject) + " text=" + flag(text) + " html=" + flag(html));
        }
    }

    private String flag(boolean ok) {
        return ok ? "§aOK" : "§cMISS";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("admin", "menu");
        }
        if (args.length == 2 && "admin".equalsIgnoreCase(args[0])) {
            return List.of("reload", "doctor", "previewmail", "force2fa", "status", "logs", "unbindmail", "resetauth", "testsmtp");
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && "previewmail".equalsIgnoreCase(args[1])) {
            return List.of("register", "reset-password", "second-factor", "test-smtp");
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && !"testsmtp".equalsIgnoreCase(args[1]) && !"reload".equalsIgnoreCase(args[1]) && !"previewmail".equalsIgnoreCase(args[1]) && !"doctor".equalsIgnoreCase(args[1])) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getName()));
            return players;
        }
        return Collections.emptyList();
    }
}
