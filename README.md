# AutoMailLogin

中文 | [English](#english)

## 中文

`AutoMailLogin` 是一个基于 `Paper` 的邮箱登录认证插件，面向 Minecraft 服务器玩家的邮箱绑定、密码登录、验证码验证与可配置二次验证场景。

### 协议

本项目使用 `MIT` 协议开源，详见 `LICENSE`。

### 项目目标

- 玩家首次进服先绑定邮箱
- 支持验证码注册、密码登录、密码重置
- 支持 `mock` 测试发信与真实 `SMTP` 发信
- 支持可配置的邮箱二次验证策略
- 默认使用 `SQLite`，并为后续 `MySQL` 扩展预留结构

### 当前已实现功能

- `/mailregister <email>`：提交邮箱并发送注册验证码
- `/mailcode <code>`：确认邮箱验证码
- `/setpassword <password> <confirm>`：设置密码并完成注册
- `/login <password>`：使用密码登录
- `/mail2fa <code>`：完成邮箱二次验证
- `/forgotpassword <email>`：发送密码重置验证码
- `/resetpassword <code> <password> <confirm>`：重置密码
- `/automaillogin admin force2fa <player>`：管理员强制下次登录触发二次验证
- `/automaillogin admin status <player>`：查看玩家认证状态
- 未登录玩家限制：移动、聊天、交互、物品栏、丢弃、非白名单命令

### 当前架构

- `AutoMailLoginPlugin`：插件入口
- `command/`：命令处理
- `listener/`：事件限制与行为拦截
- `service/`：认证、会话、验证码、邮件、二次验证服务
- `storage/`：存储抽象与 `SQLite` 实现
- `security/`：密码哈希、二次验证模式
- `src/main/resources/config.yml`：主配置
- `src/main/resources/messages.yml`：提示文本

### 当前说明

当前版本已经不是纯骨架，已经具备以下主干能力：

- 注册、登录、重置密码、二次验证的主流程
- `SQLite` 持久化账号数据
- `mock` 与 `SMTP` 两种邮件模式
- 可配置的二次验证规则模式
- 管理员基础控制命令

### 构建要求

- Java 21
- Gradle 8+

常规构建命令：

```bash
./gradlew build
```

Windows PowerShell：

```powershell
.\gradlew.bat build
```

### 下一步计划

1. 增加 `MySQLStorageProvider`
2. 增加更完整的审计日志与登录记录
3. 补充更强的管理员工具命令
4. 增加 GUI 注册与登录流程
5. 做构建验证与发布流程完善

---

## English

`AutoMailLogin` is a `Paper` plugin for Minecraft servers that provides mail-based registration, password login, verification codes, and configurable second-factor authentication.

### License

This project is released under the `MIT` License. See `LICENSE` for details.

### Goals

- Require players to bind an email on first join
- Support registration, password login, and password reset with verification codes
- Support both `mock` mail delivery and real `SMTP` delivery
- Support configurable mail-based second-factor verification rules
- Use `SQLite` by default while keeping room for future `MySQL` support

### Implemented Features

- `/mailregister <email>`: bind email and send registration code
- `/mailcode <code>`: verify the registration code
- `/setpassword <password> <confirm>`: set password and complete registration
- `/login <password>`: login with password
- `/mail2fa <code>`: complete second-factor verification
- `/forgotpassword <email>`: request password reset code
- `/resetpassword <code> <password> <confirm>`: reset password
- `/automaillogin admin force2fa <player>`: force next login to require second factor
- `/automaillogin admin status <player>`: inspect player auth state
- Restrict unauthenticated players from movement, chat, interaction, inventory usage, item dropping, and non-whitelisted commands

### Structure

- `AutoMailLoginPlugin`: plugin bootstrap
- `command/`: command handling
- `listener/`: Paper event restrictions
- `service/`: auth, session, verification, mail, and second-factor services
- `storage/`: storage abstraction and `SQLite` implementation
- `security/`: password hashing and second-factor modes
- `src/main/resources/config.yml`: main configuration
- `src/main/resources/messages.yml`: text messages

### Notes

The current version is already beyond a plain scaffold and includes:

- Main registration, login, reset-password, and second-factor flows
- Persisted account storage with `SQLite`
- `mock` and `SMTP` mail delivery modes
- Configurable second-factor modes
- Basic admin control commands

### Build

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

### Next Steps

1. Add `MySQLStorageProvider`
2. Add fuller audit logs and login records
3. Add stronger admin tooling
4. Add GUI registration and login flows
5. Improve build verification and release workflow
