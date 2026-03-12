package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import dev.wuliclaw.automaillogin.security.PasswordHasher;
import dev.wuliclaw.automaillogin.storage.StorageProvider;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthService {
    private final AutoMailLoginPlugin plugin;
    private final PlayerSessionService sessionService;
    private final VerificationService verificationService;
    private final MailService mailService;
    private final StorageProvider storageProvider;
    private final PasswordHasher passwordHasher;
    private final SecondFactorService secondFactorService;
    private final Map<UUID, Boolean> verifiedEmails = new ConcurrentHashMap<>();

    public AuthService(
            AutoMailLoginPlugin plugin,
            PlayerSessionService sessionService,
            VerificationService verificationService,
            MailService mailService,
            StorageProvider storageProvider,
            PasswordHasher passwordHasher,
            SecondFactorService secondFactorService
    ) {
        this.plugin = plugin;
        this.sessionService = sessionService;
        this.verificationService = verificationService;
        this.mailService = mailService;
        this.storageProvider = storageProvider;
        this.passwordHasher = passwordHasher;
        this.secondFactorService = secondFactorService;
    }

    public void handleJoin(Player player) {
        sessionService.markUnauthenticated(player.getUniqueId());
        player.sendMessage("§6[AutoMailLogin] §r请先登录或完成邮箱绑定。");
    }

    public void handleQuit(Player player) {
        sessionService.clear(player.getUniqueId());
        secondFactorService.clear(player.getUniqueId());
    }

    public boolean isAuthenticated(Player player) {
        return sessionService.isAuthenticated(player.getUniqueId());
    }

    public boolean isSecondFactorPending(Player player) {
        return secondFactorService.isPending(player);
    }

    public void registerMail(Player player, String email) {
        PlayerAccount account = loadOrCreate(player);
        account.setEmail(email);
        storageProvider.save(account);
        verifiedEmails.put(player.getUniqueId(), false);
        mailService.sendRegistrationCode(player, email);
    }

    public void verifyMail(Player player, String code) {
        boolean success = verificationService.verify(player.getUniqueId(), code, VerificationPurpose.REGISTER);
        if (success) {
            verifiedEmails.put(player.getUniqueId(), true);
            player.sendMessage("§a邮箱验证成功，请继续设置密码。");
            return;
        }
        player.sendMessage("§c验证码错误或已过期。");
    }

    public void setPassword(Player player, String password, String confirm) {
        if (!password.equals(confirm)) {
            player.sendMessage("§c两次输入的密码不一致。");
            return;
        }
        if (password.length() < plugin.getConfig().getInt("security.password-min-length", 8)) {
            player.sendMessage("§c密码长度不足。");
            return;
        }
        if (!verifiedEmails.getOrDefault(player.getUniqueId(), false)) {
            player.sendMessage("§c请先完成邮箱验证码确认。");
            return;
        }
        PlayerAccount account = loadOrCreate(player);
        account.setPasswordHash(passwordHasher.hash(password));
        account.setSecondFactorVerified(false);
        storageProvider.save(account);
        sessionService.markAuthenticated(player.getUniqueId());
        player.sendMessage("§a密码设置成功，已完成注册。");
    }

    public void login(Player player, String password) {
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account == null || !account.hasPassword()) {
            player.sendMessage("§c你还没有完成注册。");
            return;
        }
        if (!passwordHasher.matches(password, account.getPasswordHash())) {
            player.sendMessage("§c密码错误，请重试。");
            return;
        }
        if (account.getEmail() != null && secondFactorService.shouldRequireSecondFactor(player, account)) {
            secondFactorService.begin(player, account.getEmail(), mailService);
            player.sendMessage("§e检测到本次登录需要二次验证，请使用 /mail2fa <验证码> 完成验证。");
            return;
        }
        account.setLastIp(player.getAddress() == null ? null : player.getAddress().getAddress().getHostAddress());
        account.setSecondFactorVerified(true);
        storageProvider.save(account);
        sessionService.markAuthenticated(player.getUniqueId());
        player.sendMessage("§a登录成功，欢迎回来。");
    }

    public void verifySecondFactor(Player player, String code) {
        if (!secondFactorService.isPending(player)) {
            player.sendMessage("§c你当前没有待处理的二次验证。");
            return;
        }
        boolean success = secondFactorService.verify(player, code);
        if (!success) {
            player.sendMessage("§c二次验证验证码错误或已过期。");
            return;
        }
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account != null) {
            account.setLastIp(player.getAddress() == null ? null : player.getAddress().getAddress().getHostAddress());
            account.setSecondFactorVerified(true);
            storageProvider.save(account);
        }
        sessionService.markAuthenticated(player.getUniqueId());
        player.sendMessage("§a二次验证成功，已完成登录。");
    }

    public void forgotPassword(Player player, String email) {
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account == null || account.getEmail() == null || !account.getEmail().equalsIgnoreCase(email)) {
            player.sendMessage("§c邮箱不匹配。");
            return;
        }
        mailService.sendResetPasswordCode(player, email);
    }

    public void resetPassword(Player player, String code, String password, String confirm) {
        if (!password.equals(confirm)) {
            player.sendMessage("§c两次输入的密码不一致。");
            return;
        }
        boolean success = verificationService.verify(player.getUniqueId(), code, VerificationPurpose.RESET_PASSWORD);
        if (!success) {
            player.sendMessage("§c验证码错误或已过期。");
            return;
        }
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account == null) {
            player.sendMessage("§c账号不存在。");
            return;
        }
        account.setPasswordHash(passwordHasher.hash(password));
        account.setSecondFactorVerified(false);
        storageProvider.save(account);
        secondFactorService.markPasswordReset(player.getUniqueId());
        player.sendMessage("§a密码重置成功，请重新登录。");
    }

    public void forceSecondFactor(Player player) {
        secondFactorService.markAdminForce(player.getUniqueId());
        sessionService.markUnauthenticated(player.getUniqueId());
        player.sendMessage("§e管理员已要求你下次登录进行邮箱二次验证。");
    }

    public String describeStatus(Player player) {
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account == null) {
            return "§e玩家 " + player.getName() + " 尚未注册。";
        }
        return "§6[AutoMailLogin] §r玩家=" + player.getName()
                + " | email=" + account.getEmail()
                + " | authenticated=" + sessionService.isAuthenticated(player.getUniqueId())
                + " | secondFactorPending=" + secondFactorService.isPending(player);
    }

    private PlayerAccount loadOrCreate(Player player) {
        return storageProvider.findByUniqueId(player.getUniqueId())
                .orElseGet(() -> new PlayerAccount(player.getUniqueId(), player.getName()));
    }
}
