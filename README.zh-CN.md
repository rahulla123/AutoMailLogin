# AutoMailLogin

中文 | [English](README.en.md)

`AutoMailLogin` 是一个基于 `Paper` 的邮箱登录认证插件，面向 Minecraft 服务器玩家的邮箱绑定、密码登录、验证码验证、可配置二次验证与基础 GUI 引导场景。

## 协议

本项目使用 `MIT` 协议开源，详见 `LICENSE`。

## 项目目标

- 玩家首次进服先绑定邮箱
- 支持验证码注册、密码登录、验证码重置密码
- 支持 `mock` 测试发信与真实 `SMTP` 发信
- 支持可配置二次验证策略与信任期
- 默认使用 `SQLite`，同时支持 `MySQL`
- 提供基础 GUI 菜单，降低命令使用门槛

## 当前已实现功能

- `/mailregister <email>`：提交邮箱并发送注册验证码
- `/mailcode <code>`：确认邮箱验证码
- `/setpassword <password> <confirm>`：设置密码并完成注册
- `/login <password>`：使用密码登录
- `/mail2fa <code>`：完成邮箱二次验证
- `/forgotpassword <email>`：发送密码重置验证码
- `/resetpassword <code> <password> <confirm>`：重置密码
- `/automaillogin menu`：打开认证 GUI 菜单
- `/automaillogin admin force2fa <player>`：管理员强制下次登录触发二次验证
- `/automaillogin admin status <player>`：查看玩家认证状态与风控字段
- `/automaillogin admin logs <player>`：查看最近审计日志
- `/automaillogin admin unbindmail <player>`：解绑玩家邮箱
- `/automaillogin admin resetauth <player>`：重置玩家认证状态
- 未登录玩家限制：移动、聊天、交互、物品栏、丢弃、拾取、非白名单命令
- 登录失败次数统计与临时锁定
- 邮件重发冷却
- 二次验证信任期
- `SQLite/MySQL` 双后端与自动表结构补列

## 当前架构

- `AutoMailLoginPlugin`：插件入口
- `command/`：命令处理与 GUI 打开
- `gui/`：基础认证菜单
- `listener/`：事件限制与行为拦截
- `service/`：认证、会话、验证码、邮件、二次验证、审计服务
- `storage/`：存储抽象、SQLite、MySQL 实现
- `security/`：密码哈希、二次验证模式
- `src/main/resources/config.yml`：主配置
- `src/main/resources/messages.yml`：提示文本

## 风控与认证增强

当前版本已补上：

- 登录失败次数累计
- 达阈值后的临时锁定
- 验证码发送冷却
- 基于配置的二次验证信任期
- 管理员查看最近审计日志与扩展状态字段

## 构建要求

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

构建产物默认位于：

```text
build/libs/
```

## 建议联调清单

1. 在 `Paper 1.20.6` 服务端加载插件
2. 用 `mock` 模式走完整注册/登录/重置/2FA 流程
3. 切换到 `smtp` 模式验证真实发信
4. 验证 `sqlite` 与 `mysql` 两种存储
5. 验证管理员命令与 GUI 菜单
