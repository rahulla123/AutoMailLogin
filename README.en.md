# AutoMailLogin

[中文](README.zh-CN.md) | English

`AutoMailLogin` is a `Paper` plugin for Minecraft servers that provides mail-based registration, password login, verification codes, configurable second-factor authentication, and a basic GUI helper menu.

## License

This project is released under the `MIT` License. See `LICENSE` for details.

## Goals

- Require players to bind an email on first join
- Support registration, password login, and password reset with verification codes
- Support both `mock` mail delivery and real `SMTP` delivery
- Support configurable mail-based second-factor verification rules and trusted windows
- Use `SQLite` by default while supporting `MySQL`
- Provide a basic GUI entry for players who prefer not to start from raw commands

## Implemented Features

- `/mailregister <email>`: bind email and send registration code
- `/mailcode <code>`: verify the registration code
- `/setpassword <password> <confirm>`: set password and complete registration
- `/login <password>`: login with password
- `/mail2fa <code>`: complete second-factor verification
- `/forgotpassword <email>`: request password reset code
- `/resetpassword <code> <password> <confirm>`: reset password
- `/automaillogin menu`: open the auth GUI menu
- `/automaillogin admin force2fa <player>`: force next login to require second factor
- `/automaillogin admin status <player>`: inspect player auth and risk state
- `/automaillogin admin logs <player>`: inspect recent audit logs
- `/automaillogin admin unbindmail <player>`: unbind the player's email
- `/automaillogin admin resetauth <player>`: reset the player's auth state
- Restrict unauthenticated players from movement, chat, interaction, inventory usage, item dropping, item pickup, and non-whitelisted commands
- Login failure counting and temporary lockout
- Mail resend cooldown
- Second-factor trusted window
- SQLite/MySQL dual backend with lightweight schema migration

## Structure

- `AutoMailLoginPlugin`: plugin bootstrap
- `command/`: command handling and GUI entry
- `gui/`: basic auth menu
- `listener/`: Paper event restrictions
- `service/`: auth, session, verification, mail, second-factor, and audit services
- `storage/`: storage abstraction, SQLite, and MySQL implementations
- `security/`: password hashing and second-factor modes
- `src/main/resources/config.yml`: main configuration
- `src/main/resources/messages.yml`: text messages

## Build

Requirements:

- Java 21
- Gradle 8+

Typical build command:

```bash
./gradlew build
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
```

Build artifacts are generated in:

```text
build/libs/
```

## Suggested Validation Checklist

1. Load the plugin on a `Paper 1.20.6` server
2. Run the full register/login/reset/2FA flow in `mock` mode
3. Switch to `smtp` mode and validate real mail delivery
4. Verify both `sqlite` and `mysql` storage backends
5. Verify admin commands and the GUI menu
