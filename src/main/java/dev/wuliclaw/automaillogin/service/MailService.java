package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.mail.SmtpMailSender;
import dev.wuliclaw.automaillogin.model.PendingVerification;
import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import dev.wuliclaw.automaillogin.storage.StorageProvider;
import org.bukkit.entity.Player;

import java.time.Instant;

public final class MailService {
    private final AutoMailLoginPlugin plugin;
    private final VerificationService verificationService;
    private final SmtpMailSender smtpMailSender;
    private final StorageProvider storageProvider;
    private final MessageService messageService;

    public MailService(AutoMailLoginPlugin plugin, VerificationService verificationService, StorageProvider storageProvider, MessageService messageService) {
        this.plugin = plugin;
        this.verificationService = verificationService;
        this.smtpMailSender = new SmtpMailSender(plugin);
        this.storageProvider = storageProvider;
        this.messageService = messageService;
    }

    public void sendRegistrationCode(Player player, String email) {
        send(player, email, VerificationPurpose.REGISTER, "注册验证码", "你正在注册 AutoMailLogin，验证码为：%s\n有效期请查看插件配置。", messageService.get("mail-sent", "验证码已发送，请检查邮箱。"), messageService.get("mail-sent-mock", "测试模式：验证码已输出到控制台。"), true);
    }

    public void sendResetPasswordCode(Player player, String email) {
        send(player, email, VerificationPurpose.RESET_PASSWORD, "重置密码验证码", "你正在重置 AutoMailLogin 密码，验证码为：%s\n如果不是你本人操作，请忽略此邮件。", "§a重置验证码已发送，请检查邮箱。", "§e测试模式：重置验证码已输出到服务端控制台。", false);
    }

    public void sendSecondFactorCode(Player player, String email) {
        send(player, email, VerificationPurpose.SECOND_FACTOR, "二次验证验证码", "检测到你的账号本次登录需要二次验证，验证码为：%s", messageService.get("second-factor-required", "检测到本次登录需要二次验证，请检查邮箱验证码。"), "§e测试模式：二次验证验证码已输出到服务端控制台。", false);
    }

    private void send(Player player, String email, VerificationPurpose purpose, String subject, String mailTemplate, String smtpMessage, String mockMessage, boolean createIfMissing) {
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

        String mode = plugin.getConfig().getString("mail.mode", "mock");
        if ("smtp".equalsIgnoreCase(mode)) {
            boolean success = smtpMailSender.send(email, subject, mailTemplate.formatted(verification.code()));
            if (success) {
                player.sendMessage(smtpMessage);
            } else {
                player.sendMessage("§c邮件发送失败，请检查 SMTP 配置或稍后重试。");
            }
            return;
        }
        plugin.getLogger().info("[MOCK MAIL] " + subject + " for " + player.getName() + " <" + email + ">: " + verification.code());
        player.sendMessage(mockMessage);
    }
}
