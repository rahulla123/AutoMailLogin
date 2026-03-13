# AutoMailLogin

中文 | [English](README.en.md)

`AutoMailLogin` 是一个基于 `Paper` 的 Minecraft 邮箱认证插件，目标是把传统的口令登录流程升级成更安全、更适合正式服务器使用的邮箱认证体系。

## 插件优势

- 邮箱注册与验证码确认结合，降低盗号与撞库风险
- 支持密码登录、密码重置、邮箱二次验证，认证链路更完整
- 支持 `SQLite / MySQL` 双存储，适配本地测试与正式服务器
- 内置审计日志，方便管理员排查认证相关问题
- 提供基础 GUI 菜单，降低玩家首次使用门槛
- 支持登录失败锁定、验证码发送冷却、二次验证信任期等基础风控能力

## 核心功能

- 邮箱注册
- 邮箱验证码确认
- 密码设置与密码登录
- 忘记密码与验证码重置
- 邮箱二次验证（2FA）
- 认证前行为限制
- 审计日志记录
- 管理员认证状态干预
- 基础 GUI 认证菜单

## 已实现命令

### 玩家命令

- `/mailregister <email>`：绑定邮箱并发送注册验证码
- `/mailcode <code>`：确认注册验证码
- `/setpassword <password> <confirm>`：设置登录密码
- `/login <password>`：使用密码登录
- `/mail2fa <code>`：完成邮箱二次验证
- `/forgotpassword <email>`：发送密码重置验证码
- `/resetpassword <code> <password> <confirm>`：重置密码
- `/automaillogin menu`：打开认证 GUI 菜单

### 管理员命令

- `/automaillogin admin force2fa <player>`：强制目标玩家下次登录触发二次验证
- `/automaillogin admin status <player>`：查看玩家认证状态
- `/automaillogin admin logs <player>`：查看最近审计日志
- `/automaillogin admin unbindmail <player>`：解绑玩家邮箱
- `/automaillogin admin resetauth <player>`：重置玩家认证状态

## 权限节点

- `automaillogin.admin`
  - 默认：`op`
  - 说明：允许使用全部管理员命令

## 适用场景

- 需要提升玩家账号安全性的生存服/公益服
- 希望增加邮箱找回与二次验证能力的正式服务器
- 需要从本地 `SQLite` 平滑升级到 `MySQL` 的项目
- 需要基础审计日志与管理员认证控制能力的服务端环境
