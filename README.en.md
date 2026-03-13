# AutoMailLogin

[中文](README.zh-CN.md) | English

`AutoMailLogin` is a `Paper` plugin for Minecraft servers that upgrades traditional password-only login into a more secure mail-based authentication flow.

## Advantages

- Combines email binding and verification codes to reduce account theft risk
- Supports password login, password reset, and mail-based second-factor authentication
- Supports both `SQLite` and `MySQL` for small servers and production deployments
- Includes audit logs for authentication-related administration and troubleshooting
- Provides a basic GUI menu to help players start the auth flow more easily
- Includes practical risk controls such as login lockout, resend cooldown, and second-factor trusted windows

## Core Features

- Email registration
- Verification code confirmation
- Password setup and password login
- Forgot-password and code-based password reset
- Mail-based second-factor authentication (2FA)
- Pre-auth player restrictions
- Audit log recording
- Admin auth-state intervention
- Basic GUI auth menu

## Implemented Commands

### Player Commands

- `/mailregister <email>`: bind email and send a registration code
- `/mailcode <code>`: verify the registration code
- `/setpassword <password> <confirm>`: set the login password
- `/login <password>`: login with password
- `/mail2fa <code>`: complete second-factor verification
- `/forgotpassword <email>`: send a password reset code
- `/resetpassword <code> <password> <confirm>`: reset the password
- `/automaillogin menu`: open the auth GUI menu

### Admin Commands

- `/automaillogin admin force2fa <player>`: force second-factor verification on the next login
- `/automaillogin admin status <player>`: inspect player auth state
- `/automaillogin admin logs <player>`: inspect recent audit logs
- `/automaillogin admin unbindmail <player>`: unbind the player's email
- `/automaillogin admin resetauth <player>`: reset the player's auth state

## Permission Node

- `automaillogin.admin`
  - Default: `op`
  - Grants access to all admin commands

## Suitable Use Cases

- Survival or public servers that want stronger account protection
- Production servers that need password reset and second-factor verification
- Projects that want to start with `SQLite` and later move to `MySQL`
- Server environments that need audit logs and admin-level auth controls
