# AutoMailLogin

[中文](README.md) | English

> 🔐 A mail-based authentication plugin for `Paper` servers. It upgrades the traditional password-only login flow into a more complete and secure experience with email verification, password reset, mail-based 2FA, admin audit tools, and editable mail templates.

## ✨ What does it solve?

Many traditional login plugins stop at "register a password and type it again later". That works, but it is weak when players forget passwords, accounts get stolen, or admins need better visibility.

`AutoMailLogin` is built to solve that:

- 📬 Email-based registration verification
- 🔐 Mail-based second-factor authentication (2FA)
- ♻️ Forgot-password and reset-password flow
- 🧾 Audit logs for admin troubleshooting
- 🧩 GUI entry for easier player onboarding
- 🗂️ `SQLite / MySQL` support for both testing and production
- 🎨 Editable text / HTML mail templates

## 🚀 Key Advantages

- **More secure**: verification codes, 2FA, lockout rules, resend cooldown
- **More complete**: registration, login, reset, audit, and admin intervention in one flow
- **More user-friendly**: includes a GUI menu for players
- **More flexible**: supports both `SQLite` and `MySQL`
- **More customizable**: editable mail templates with text + HTML support
- **More production-ready**: admins can inspect auth state, logs, and SMTP delivery

## 📊 Compared to traditional password login

| Item | Traditional login | AutoMailLogin |
|---|---:|---:|
| Email registration verification | ❌ | ✅ |
| Forgot-password recovery | ⚠️ Often weak | ✅ |
| Mail-based 2FA | ❌ | ✅ |
| Login failure lockout | ⚠️ Not always available | ✅ |
| GUI guidance | ❌ | ✅ |
| Audit logs | ❌ | ✅ |
| SQLite / MySQL support | ⚠️ Depends on implementation | ✅ |
| Editable mail templates | ❌ | ✅ |
| HTML mail support | ❌ | ✅ |

## 👤 How do players use it?

### First-time registration

1. Use `/mailregister <email>` to bind an email
2. Enter `/mailcode <code>` after receiving the verification code
3. Set a password with `/setpassword <password> <confirm>`
4. Registration is completed and the player becomes authenticated

### Normal login

1. Use `/login <password>`
2. If second-factor verification is triggered, use `/mail2fa <code>`

### Forgot password

1. Use `/forgotpassword <email>`
2. After receiving the code, use `/resetpassword <code> <newPassword> <confirm>`

### GUI menu flow

Players can also open:

- `/automaillogin menu`

Then click a menu item and continue the flow through chat prompts.

## 🧩 Player-side protection

Before authentication, the plugin can restrict:

- movement
- chat
- interaction
- inventory usage
- item dropping
- item pickup
- non-whitelisted commands

It also supports:

- login failure counting
- temporary lockout after too many failures
- resend cooldown for verification codes
- trusted window for second-factor verification
- persisted verification state to reduce flow loss after restarts

## 🛠️ What can admins do?

### Admin commands

- `/automaillogin admin status <player>`: inspect player auth state
- `/automaillogin admin logs <player>`: inspect recent audit logs
- `/automaillogin admin force2fa <player>`: force 2FA on next login
- `/automaillogin admin unbindmail <player>`: unbind the player's email
- `/automaillogin admin resetauth <player>`: reset player auth state
- `/automaillogin admin testsmtp <email>`: send a test mail with current SMTP settings

### Admin intervention abilities

- force second-factor verification
- reset current auth state
- clear bound email
- inspect recent auth logs
- inspect verification state, pending code state, recent login info, and more

## 🔑 Permission Node

| Permission | Default | Description |
|---|---:|---|
| `automaillogin.admin` | `op` | Grants access to all admin commands |

## ⚙️ Core Configuration

| Config key | Purpose | Example |
|---|---|---|
| `database.type` | Select storage backend | `sqlite` / `mysql` |
| `mail.mode` | Mail delivery mode | `mock` / `smtp` |
| `mail.smtp.host` | SMTP server address | `smtp.example.com` |
| `mail.server-name` | Server name used in templates | `My Server` |
| `mail.support-email` | Support contact used in templates | `support@example.com` |
| `mail.template-dir` | Mail template directory | `templates` |
| `security.second-factor.mode` | 2FA trigger mode | `on_new_ip` |
| `security.max-login-attempts` | Max failed attempts | `5` |
| `security.lock-seconds` | Lock duration | `300` |

## 📬 Mail Template System

`AutoMailLogin` includes an editable mail template system.

### Features

- supports both **text templates** and **HTML templates**
- templates are auto-generated into the plugin data directory
- server owners can edit them directly
- supports `${var}` placeholder replacement

### Template directory

Templates are generated in:

- `plugins/AutoMailLogin/templates/`

### Included templates

- `register.subject.txt`
- `register.text.txt`
- `register.html`
- `reset-password.subject.txt`
- `reset-password.text.txt`
- `reset-password.html`
- `second-factor.subject.txt`
- `second-factor.text.txt`
- `second-factor.html`
- `test-smtp.subject.txt`
- `test-smtp.text.txt`
- `test-smtp.html`

### Available variables

- `${code}`
- `${player}`
- `${email}`
- `${server_name}`
- `${expire_seconds}`
- `${support_email}`

## 🎯 Best fit for

- survival or public servers that want better account security
- production servers that need password recovery and 2FA
- projects that want to start with `SQLite` and later move to `MySQL`
- teams that need audit visibility and admin auth controls
- server owners who want editable and brandable mail templates

## ✅ Summary

If you want something better than an old-school password-only login plugin, and you want a more complete mail-based authentication workflow that can actually run on a real `Paper` server, `AutoMailLogin` is built for that job.
