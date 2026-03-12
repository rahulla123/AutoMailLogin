package dev.wuliclaw.automaillogin;

import dev.wuliclaw.automaillogin.command.AdminCommand;
import dev.wuliclaw.automaillogin.command.AuthCommand;
import dev.wuliclaw.automaillogin.listener.AuthRestrictionListener;
import dev.wuliclaw.automaillogin.security.PasswordHasher;
import dev.wuliclaw.automaillogin.service.AuthService;
import dev.wuliclaw.automaillogin.service.MailService;
import dev.wuliclaw.automaillogin.service.PlayerSessionService;
import dev.wuliclaw.automaillogin.service.SecondFactorService;
import dev.wuliclaw.automaillogin.service.VerificationService;
import dev.wuliclaw.automaillogin.storage.SQLiteStorageProvider;
import dev.wuliclaw.automaillogin.storage.StorageProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AutoMailLoginPlugin extends JavaPlugin {
    private PlayerSessionService playerSessionService;
    private VerificationService verificationService;
    private MailService mailService;
    private AuthService authService;
    private StorageProvider storageProvider;
    private SecondFactorService secondFactorService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.playerSessionService = new PlayerSessionService();
        this.verificationService = new VerificationService(this);
        this.storageProvider = new SQLiteStorageProvider(this);
        this.storageProvider.initialize();
        this.secondFactorService = new SecondFactorService(this, verificationService);
        this.mailService = new MailService(this, verificationService);
        this.authService = new AuthService(this, playerSessionService, verificationService, mailService, storageProvider, new PasswordHasher(), secondFactorService);

        AuthCommand authCommand = new AuthCommand(authService);
        AdminCommand adminCommand = new AdminCommand(authService);
        registerCommand("mailregister", authCommand);
        registerCommand("mailcode", authCommand);
        registerCommand("setpassword", authCommand);
        registerCommand("login", authCommand);
        registerCommand("forgotpassword", authCommand);
        registerCommand("resetpassword", authCommand);
        registerCommand("mail2fa", authCommand);
        registerCommand("automaillogin", adminCommand);

        getServer().getPluginManager().registerEvents(
                new AuthRestrictionListener(this, playerSessionService, authService),
                this
        );
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            if (executor instanceof AuthCommand authCommand) {
                command.setExecutor(authCommand);
                command.setTabCompleter(authCommand);
            } else if (executor instanceof AdminCommand adminCommand) {
                command.setExecutor(adminCommand);
                command.setTabCompleter(adminCommand);
            }
        }
    }
}
