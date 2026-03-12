# AutoMailLogin

`AutoMailLogin` is a Paper plugin for mail-based player registration and login.

## License

This project is released under the MIT License. See `LICENSE`.

## Goals

- First login requires email binding
- Verification code supports mock mode and SMTP mode
- Password login remains the primary flow
- Optional mail-based second factor can be enabled by rules
- SQLite by default, with MySQL reserved in config design

## MVP Scope

- `/mailregister <email>`
- `/mailcode <code>`
- `/setpassword <password> <confirm>`
- `/login <password>`
- `/mail2fa <code>`
- `/forgotpassword <email>`
- `/resetpassword <code> <password> <confirm>`
- `/automaillogin admin force2fa <player>`
- `/automaillogin admin status <player>`
- Restrict unauthenticated players from moving, chatting, interacting, dropping and using non-whitelisted commands

## Project Structure

- `AutoMailLoginPlugin` - plugin bootstrap
- `command/` - command handling
- `listener/` - Paper event restrictions
- `service/` - auth, session, mail and verification services
- `model/` - lightweight domain models
- `src/main/resources/config.yml` - config skeleton
- `src/main/resources/messages.yml` - text output

## Notes

Current version is a scaffolded MVP architecture.

- Mail sending now covers register, reset-password, and second-factor flows in mock mode
- SMTP sending is now wired through `jakarta.mail`, using the plugin config for host, port, TLS and credentials
- Storage now uses SQLite by default for persisted accounts
- GUI flows, MySQL-backed storage, and stronger second-factor rules are the next logical steps

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

## Next Steps

1. Add `MySQLStorageProvider`
2. Integrate real SMTP sending
3. Add stronger audit logs and admin tooling
4. Add MySQL-backed storage
5. Add GUI registration and login flows
