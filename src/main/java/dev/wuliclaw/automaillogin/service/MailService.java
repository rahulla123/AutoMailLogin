package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.mail.SmtpMailSender;
import dev.wuliclaw.automaillogin.model.PendingVerification;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import org.bukkit.entity.Player;

public final class MailService {
    private final AutoMailLoginPlugin plugin;
    private final VerificationService verificationService;
    private final SmtpMailSender smtpMailSender;

    public MailService(AutoMailLoginPlugin plugin, VerificationService verificationService) {
        this.plugin = plugin;
        this.verificationService = verificationService;
        this.smtpMailSender = new SmtpMailSender(plugin);
    }

    public void sendRegistrationCode(Player player, String email) {
        send(player, email, VerificationPurpose.REGISTER, "注册验证码", "你正在注册 AutoMailLogin，验证码为：%s\n有效期请查看插件配置。", "§a验证码已发送，请检查邮箱。", "§e测试模式：注册验证码已输出到服务端控制台。");
    }

    public void sendResetPasswordCode(Player player, String email) {
        send(player, email, VerificationPurpose.RESET_PASSWORD, "重置密码验证码", "你正在重置 AutoMailLogin 密码，验证码为：%s\n如果不是你本人操作，请忽略此邮件。", "§a重置验证码已发送，请检查邮箱。", "§e测试模式：重置验证码已输出到服务端控制台。");
    }

    public void sendSecondFactorCode(Player player, String email) {
        send(player, email, VerificationPurpose.SECOND_FACTOR, "二次验证验证码", "检测到你的账号本次登录需要二次验证，验证码为：%s", "§e检测到本次登录需要二次验证，请检查邮箱验证码。", "§e测试模式：二次验证验证码已输出到服务端控制台。");
    }

    private void send(
            Player player,
            String email,
            VerificationPurpose purpose,
            String subject,
            String mailTemplate,
            String smtpMessage,
            String mockMessage
    ) {
        PendingVerification verification = verificationService.create(player.getUniqueId(), email, purpose);
        String mode = plugin.getConfig().getString("mail.mode", "mock");
        if ("smtp".equalsIgnoreCase(mode)) {
            smtpMailSender.send(email, subject, mailTemplate.formatted(verification.code()));
            player.sendMessage(smtpMessage);
            return;
        }
        plugin.getLogger().info("[MOCK MAIL] " + subject + " for " + player.getName() + " <" + email + ">: " + verification.code());
        player.sendMessage(mockMessage);
    }
}
