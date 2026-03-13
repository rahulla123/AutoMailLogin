package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.mail.MailTemplateService;
import dev.wuliclaw.automaillogin.mail.MailTemplateType;
import dev.wuliclaw.automaillogin.mail.RenderedMailTemplate;
import dev.wuliclaw.automaillogin.mail.SmtpMailSender;
import dev.wuliclaw.automaillogin.model.PendingVerification;
import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import dev.wuliclaw.automaillogin.storage.StorageProvider;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

public final class MailService {
    private final AutoMailLoginPlugin plugin;
    private final VerificationService verificationService;
    private final SmtpMailSender smtpMailSender;
    private final StorageProvider storageProvider;
    private final MessageService messageService;
    private final MailTemplateService templateService;

    public MailService(AutoMailLoginPlugin plugin, VerificationService verificationService, StorageProvider storageProvider, MessageService messageService, MailTemplateService templateService) {
        this.plugin = plugin;
        this.verificationService = verificationService;
        this.smtpMailSender = new SmtpMailSender(plugin);
        this.storageProvider = storageProvider;
        this.messageService = messageService;
        this.templateService = templateService;
    }

    public void sendRegistrationCode(Player player, String email) {
        send(player, email, VerificationPurpose.REGISTER, MailTemplateType.REGISTER, messageService.get("mail-sent", "验证码已发送，请检查邮箱。"), messageService.get("mail-sent-mock", "测试模式：验证码已输出到控制台。"), true);
    }

    public void sendResetPasswordCode(Player player, String email) {
        send(player, email, VerificationPurpose.RESET_PASSWORD, MailTemplateType.RESET_PASSWORD, "§a重置验证码已发送，请检查邮箱。", "§e测试模式：重置验证码已输出到服务端控制台。", false);
    }

    public void sendSecondFactorCode(Player player, String email) {
        send(player, email, VerificationPurpose.SECOND_FACTOR, MailTemplateType.SECOND_FACTOR, messageService.get("second-factor-required", "检测到本次登录需要二次验证，请检查邮箱验证码。"), "§e测试模式：二次验证验证码已输出到服务端控制台。", false);
    }

    public void sendTestMailAsync(String email, Consumer<Boolean> callback) {
        sendPreviewMailAsync(MailTemplateType.TEST_SMTP, email, callback);
    }

    public void sendPreviewMailAsync(MailTemplateType templateType, String email, Consumer<Boolean> callback) {
        RenderedMailTemplate template = templateService.render(templateType, baseVariables(null, email, null));
        String mode = plugin.getConfig().getString("mail.mode", "mock");
        if (!"smtp".equalsIgnoreCase(mode)) {
            logMockMail(template, null, email);
            callback.accept(true);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = smtpMailSender.send(email, template);
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(success));
        });
    }

    private void send(Player player, String email, VerificationPurpose purpose, MailTemplateType templateType, String smtpMessage, String mockMessage, boolean createIfMissing) {
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account == null && createIfMissing) {
            account = new PlayerAccount(player.getUniqueId(), player.getName());
        }
        if (account != null && !verificationService.canSend(player.getUniqueId())) {
            long remaining = verificationService.getRemainingCooldownSeconds(player.getUniqueId());
            player.sendMessage("§c发送过于频繁，请在 " + remaining + " 秒后再试。");
            return;
        }

        PendingVerification verification = verificationService.create(player.getUniqueId(), email, purpose);
        if (account != null) {
            account.setLastCodeSentAt(Instant.now());
            if (email != null && (account.getEmail() == null || createIfMissing)) {
                account.setEmail(email);
            }
            storageProvider.save(account);
        }

        RenderedMailTemplate template = templateService.render(templateType, baseVariables(player, email, verification));
        String mode = plugin.getConfig().getString("mail.mode", "mock");
        if ("smtp".equalsIgnoreCase(mode)) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean success = smtpMailSender.send(email, template);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (success) {
                        player.sendMessage(smtpMessage);
                    } else {
                        player.sendMessage("§c邮件发送失败，请检查 SMTP 配置或稍后重试。");
                    }
                });
            });
            return;
        }
        logMockMail(template, player.getName(), email);
        player.sendMessage(mockMessage);
    }

    private void logMockMail(RenderedMailTemplate template, String playerName, String email) {
        boolean logSensitive = plugin.getConfig().getBoolean("mail.mock-log-sensitive-content", false);
        plugin.getLogger().info("[MOCK MAIL][SUBJECT] " + (logSensitive ? template.subject() : sanitizeForMock(template.subject())));
        plugin.getLogger().info("[MOCK MAIL][TEXT] " + (logSensitive ? template.textBody() : sanitizeForMock(template.textBody())));
        if (template.htmlBody() != null) {
            plugin.getLogger().info("[MOCK MAIL][HTML] " + (logSensitive ? template.htmlBody() : sanitizeForMock(template.htmlBody())));
        }
        if (playerName != null || email != null) {
            plugin.getLogger().info("[MOCK MAIL][META] player=" + (playerName == null ? "-" : playerName) + ", email=" + (logSensitive ? email : maskEmail(email)));
        }
    }

    private String sanitizeForMock(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = value.replaceAll("\\b\\d{6,8}\\b", "******");
        sanitized = sanitized.replaceAll("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})", "$1***@$2");
        return sanitized;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(Math.max(atIndex, 0));
        }
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    private Map<String, String> baseVariables(Player player, String email, PendingVerification verification) {
        return Map.of(
                "code", verification == null ? "TEST-CODE" : verification.code(),
                "player", player == null ? "Player" : player.getName(),
                "email", email == null ? "" : email,
                "server_name", plugin.getConfig().getString("mail.server-name", "AutoMailLogin Server"),
                "expire_seconds", String.valueOf(plugin.getConfig().getInt("mail.code-expire-seconds", 300)),
                "support_email", plugin.getConfig().getString("mail.support-email", "support@example.com")
        );
    }
}
