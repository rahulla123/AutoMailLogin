package dev.wuliclaw.automaillogin.service;

import dev.wuliclaw.automaillogin.AutoMailLoginPlugin;
import dev.wuliclaw.automaillogin.model.PlayerAccount;
import dev.wuliclaw.automaillogin.model.VerificationPurpose;
import dev.wuliclaw.automaillogin.security.PasswordHasher;
import dev.wuliclaw.automaillogin.storage.AuditLogEntry;
import dev.wuliclaw.automaillogin.storage.StorageProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private final AutoMailLoginPlugin plugin;
    private final PlayerSessionService sessionService;
    private final VerificationService verificationService;
    private final MailService mailService;
    private final StorageProvider storageProvider;
    private final PasswordHasher passwordHasher;
    private final SecondFactorService secondFactorService;
    private final AuditLogService auditLogService;
    private final Map<UUID, Boolean> verifiedEmails = new ConcurrentHashMap<>();

    public AuthService(
            AutoMailLoginPlugin plugin,
            PlayerSessionService sessionService,
            VerificationService verificationService,
            MailService mailService,
            StorageProvider storageProvider,
            PasswordHasher passwordHasher,
            SecondFactorService secondFactorService,
            AuditLogService auditLogService
    ) {
        this.plugin = plugin;
        this.sessionService = sessionService;
        this.verificationService = verificationService;
        this.mailService = mailService;
        this.storageProvider = storageProvider;
        this.passwordHasher = passwordHasher;
        this.secondFactorService = secondFactorService;
        this.auditLogService = auditLogService;
    }

    public void handleJoin(Player player) {
        sessionService.markUnauthenticated(player.getUniqueId());
        auditLogService.log(player, "PLAYER_JOIN", "Player joined and entered unauthenticated state");
        player.sendMessage("§6[AutoMailLogin] §r请先登录或完成邮箱绑定。输入 /automaillogin menu 可打开认证菜单。");
    }

    public void handleQuit(Player player) {
        sessionService.clear(player.getUniqueId());
        secondFactorService.clear(player.getUniqueId());
        auditLogService.log(player, "PLAYER_QUIT", "Player quit and session cleared");
    }

    public boolean isAuthenticated(Player player) {
        return sessionService.isAuthenticated(player.getUniqueId());
    }

    public boolean isSecondFactorPending(Player player) {
        return secondFactorService.isPending(player);
    }

    public void registerMail(Player player, String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            player.sendMessage("§c邮箱格式不正确。");
            return;
        }
        PlayerAccount account = loadOrCreate(player);
        account.setEmail(email);
        account.setTrustedUntil(null);
        storageProvider.save(account);
        verifiedEmails.put(player.getUniqueId(), false);
        auditLogService.log(player, "REGISTER_MAIL", "Email submitted: " + email);
        mailService.sendRegistrationCode(player, email);
    }

    public void verifyMail(Player player, String code) {
        boolean success = verificationService.verify(player.getUniqueId(), code, VerificationPurpose.REGISTER);
        if (success) {
            verifiedEmails.put(player.getUniqueId(), true);
            auditLogService.log(player, "VERIFY_MAIL_SUCCESS", "Registration mail verification passed");
            player.sendMessage("§a邮箱验证成功，请继续设置密码。输入 /setpassword <密码> <确认密码>");
            return;
        }
        auditLogService.log(player, "VERIFY_MAIL_FAILED", "Registration mail verification failed");
        player.sendMessage("§c验证码错误或已过期。");
    }

    public void setPassword(Player player, String password, String confirm) {
        if (!password.equals(confirm)) {
            auditLogService.log(player, "SET_PASSWORD_FAILED", "Password confirmation mismatch");
            player.sendMessage("§c两次输入的密码不一致。");
            return;
        }
        if (password.length() < plugin.getConfig().getInt("security.password-min-length", 8)) {
            auditLogService.log(player, "SET_PASSWORD_FAILED", "Password too short");
            player.sendMessage("§c密码长度不足。");
            return;
        }
        if (!verifiedEmails.getOrDefault(player.getUniqueId(), false)) {
            auditLogService.log(player, "SET_PASSWORD_FAILED", "Email not verified before password set");
            player.sendMessage("§c请先完成邮箱验证码确认。");
            return;
        }
        PlayerAccount account = loadOrCreate(player);
        account.setPasswordHash(passwordHasher.hash(password));
        account.setSecondFactorVerified(false);
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        storageProvider.save(account);
        sessionService.markAuthenticated(player.getUniqueId());
        auditLogService.log(player, "SET_PASSWORD_SUCCESS", "Password set and registration completed");
        player.sendMessage("§a密码设置成功，已完成注册。");
    }

    public void login(Player player, String password) {
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account == null || !account.hasPassword()) {
            auditLogService.log(player, "LOGIN_FAILED", "Player attempted login without registration");
            player.sendMessage("§c你还没有完成注册。");
            return;
        }
        if (account.isLocked()) {
            auditLogService.log(player, "LOGIN_BLOCKED", "Player is temporarily locked");
            player.sendMessage("§c登录已被临时锁定，请稍后再试。解锁时间：" + account.getLockedUntil());
            return;
        }
        if (!passwordHasher.matches(password, account.getPasswordHash())) {
            int maxAttempts = plugin.getConfig().getInt("security.max-login-attempts", 5);
            int lockSeconds = plugin.getConfig().getInt("security.lock-seconds", 300);
            account.setFailedLoginAttempts(account.getFailedLoginAttempts() + 1);
            if (account.getFailedLoginAttempts() >= maxAttempts) {
                account.setLockedUntil(Instant.now().plusSeconds(lockSeconds));
                account.setFailedLoginAttempts(0);
            }
            storageProvider.save(account);
            auditLogService.log(player, "LOGIN_FAILED", "Password mismatch");
            player.sendMessage("§c密码错误，请重试。");
            return;
        }
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        if (account.getEmail() != null && secondFactorService.shouldRequireSecondFactor(player, account)) {
            storageProvider.save(account);
            secondFactorService.begin(player, account.getEmail(), mailService);
            auditLogService.log(player, "LOGIN_2FA_REQUIRED", "Second factor required by rule");
            player.sendMessage("§e检测到本次登录需要二次验证，请使用 /mail2fa <验证码> 完成验证。");
            return;
        }
        completeLogin(player, account);
        auditLogService.log(player, "LOGIN_SUCCESS", "Player logged in successfully");
        player.sendMessage("§a登录成功，欢迎回来。");
    }

    public void verifySecondFactor(Player player, String code) {
        if (!secondFactorService.isPending(player)) {
            auditLogService.log(player, "VERIFY_2FA_FAILED", "No pending second factor session");
            player.sendMessage("§c你当前没有待处理的二次验证。");
            return;
        }
        boolean success = secondFactorService.verify(player, code);
        if (!success) {
            auditLogService.log(player, "VERIFY_2FA_FAILED", "Second factor code invalid or expired");
            player.sendMessage("§c二次验证验证码错误或已过期。");
            return;
        }
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account != null) {
            completeLogin(player, account);
        }
        auditLogService.log(player, "VERIFY_2FA_SUCCESS", "Second factor verified and login completed");
        player.sendMessage("§a二次验证成功，已完成登录。");
    }

    public void forgotPassword(Player player, String email) {
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account == null || account.getEmail() == null || !account.getEmail().equalsIgnoreCase(email)) {
            auditLogService.log(player, "FORGOT_PASSWORD_FAILED", "Email mismatch during reset request");
            player.sendMessage("§c邮箱不匹配。");
            return;
        }
        auditLogService.log(player, "FORGOT_PASSWORD_REQUEST", "Password reset code requested");
        mailService.sendResetPasswordCode(player, email);
    }

    public void resetPassword(Player player, String code, String password, String confirm) {
        if (!password.equals(confirm)) {
            auditLogService.log(player, "RESET_PASSWORD_FAILED", "Password confirmation mismatch");
            player.sendMessage("§c两次输入的密码不一致。");
            return;
        }
        boolean success = verificationService.verify(player.getUniqueId(), code, VerificationPurpose.RESET_PASSWORD);
        if (!success) {
            auditLogService.log(player, "RESET_PASSWORD_FAILED", "Reset verification code invalid or expired");
            player.sendMessage("§c验证码错误或已过期。");
            return;
        }
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account == null) {
            auditLogService.log(player, "RESET_PASSWORD_FAILED", "Account not found");
            player.sendMessage("§c账号不存在。");
            return;
        }
        account.setPasswordHash(passwordHasher.hash(password));
        account.setSecondFactorVerified(false);
        account.setTrustedUntil(null);
        storageProvider.save(account);
        secondFactorService.markPasswordReset(player.getUniqueId());
        auditLogService.log(player, "RESET_PASSWORD_SUCCESS", "Password reset completed");
        player.sendMessage("§a密码重置成功，请重新登录。");
    }

    public void forceSecondFactor(Player player) {
        secondFactorService.markAdminForce(player.getUniqueId());
        PlayerAccount account = loadOrCreate(player);
        account.setTrustedUntil(null);
        storageProvider.save(account);
        sessionService.markUnauthenticated(player.getUniqueId());
        auditLogService.log(player, "ADMIN_FORCE_2FA", "Admin marked next login for second factor");
        player.sendMessage("§e管理员已要求你下次登录进行邮箱二次验证。");
    }

    public void unbindEmail(Player player) {
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account == null) {
            return;
        }
        account.setEmail(null);
        account.setSecondFactorVerified(false);
        account.setTrustedUntil(null);
        storageProvider.save(account);
        sessionService.markUnauthenticated(player.getUniqueId());
        auditLogService.log(player, "ADMIN_UNBIND_EMAIL", "Admin removed bound email");
        player.sendMessage("§e你的绑定邮箱已被管理员解除，请重新绑定。");
    }

    public void resetAuthState(Player player) {
        sessionService.markUnauthenticated(player.getUniqueId());
        secondFactorService.clear(player.getUniqueId());
        PlayerAccount account = storageProvider.findByUniqueId(player.getUniqueId()).orElse(null);
        if (account != null) {
            account.setTrustedUntil(null);
            account.setLockedUntil(null);
            account.setFailedLoginAttempts(0);
            storageProvider.save(account);
        }
        auditLogService.log(player, "ADMIN_RESET_AUTH_STATE", "Admin reset player auth/session state");
        player.sendMessage("§e你的认证状态已被管理员重置，请重新登录。");
    }

    public String describeStatusByName(String playerName) {
        PlayerAccount account = storageProvider.findByPlayerName(playerName).orElse(null);
        if (account == null) {
            return "§e玩家 " + playerName + " 尚未注册。";
        }
        boolean onlineAuthenticated = false;
        Player online = plugin.getServer().getPlayerExact(playerName);
        if (online != null) {
            onlineAuthenticated = sessionService.isAuthenticated(online.getUniqueId());
        }
        return "§6[AutoMailLogin] §r玩家=" + account.getPlayerName()
                + " | email=" + account.getEmail()
                + " | registeredAt=" + account.getRegisteredAt()
                + " | lastLoginAt=" + account.getLastLoginAt()
                + " | lastIp=" + account.getLastIp()
                + " | failedAttempts=" + account.getFailedLoginAttempts()
                + " | lockedUntil=" + account.getLockedUntil()
                + " | trustedUntil=" + account.getTrustedUntil()
                + " | authenticated=" + onlineAuthenticated;
    }

    public String describeStatus(Player player) {
        return describeStatusByName(player.getName());
    }

    public List<AuditLogEntry> getRecentLogs(Player player, int limit) {
        return auditLogService.getRecent(player.getUniqueId(), limit);
    }

    public List<AuditLogEntry> getRecentLogsByName(String playerName, int limit) {
        PlayerAccount account = storageProvider.findByPlayerName(playerName).orElse(null);
        if (account == null) {
            return List.of();
        }
        return auditLogService.getRecent(account.getUniqueId(), limit);
    }

    public PlayerAccount findAccountByName(String playerName) {
        return storageProvider.findByPlayerName(playerName).orElse(null);
    }

    private void completeLogin(Player player, PlayerAccount account) {
        account.setLastIp(player.getAddress() == null ? null : player.getAddress().getAddress().getHostAddress());
        account.setSecondFactorVerified(true);
        account.setLastLoginAt(Instant.now());
        account.setFailedLoginAttempts(0);
        account.setLockedUntil(null);
        storageProvider.save(account);
        sessionService.markAuthenticated(player.getUniqueId());
    }

    private PlayerAccount loadOrCreate(Player player) {
        return storageProvider.findByUniqueId(player.getUniqueId())
                .orElseGet(() -> new PlayerAccount(player.getUniqueId(), player.getName()));
    }
}
