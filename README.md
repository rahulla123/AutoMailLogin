# AutoMailLogin

中文 | [English](README.en.md)

> 🔐 一个专为 **Minecraft `Paper` 服务器** 打造的邮箱认证插件。它把传统“只靠密码”的登录方式升级成更安全、更完整的认证流程：邮箱验证、密码登录、验证码重置、邮件二次验证（2FA）、管理员审计与可编辑邮件模板。

## 🧭 这是什么？

`AutoMailLogin` 是一个用于 **Minecraft 服务器玩家认证** 的插件，运行在 `Paper` 服务端上。

它不是普通的网站登录模块，也不是通用邮件工具，而是专门给服主解决下面这些问题：

- 玩家注册后如何绑定邮箱
- 玩家忘记密码后如何安全找回
- 玩家异地登录时如何追加二次验证
- 管理员如何查看认证状态、日志和风险情况

## ✨ 它能解决什么问题？

很多服务器的传统登录方案只有“注册密码 + 输入密码”这一层，一旦玩家忘记密码、撞库、账号被盗，处理起来就会很麻烦。

`AutoMailLogin` 想解决的正是这些问题：

- 📬 用邮箱完成注册验证，而不是只靠口令
- 🔐 支持登录后的邮件二次验证（2FA）
- ♻️ 支持忘记密码与验证码重置流程
- 🧾 记录审计日志，方便管理员排查问题
- 🧩 提供 GUI 菜单，降低玩家使用门槛
- 🗂️ 支持 `SQLite / MySQL`，适合从测试服到正式服
- 🎨 支持可编辑的文本 / HTML 邮件模板

## 🚀 核心优势

- **更安全**：支持邮箱验证、二次验证、失败锁定、验证码冷却
- **更完整**：覆盖注册、登录、找回密码、重置密码、审计与管理链路
- **更易用**：支持 GUI 菜单，玩家不必死记所有命令
- **更灵活**：支持 `SQLite / MySQL` 双后端
- **更可运营**：邮件模板支持文本 + HTML，可直接在插件目录里自定义
- **更适合正式服务器**：管理员可查看状态、日志、重置认证、解绑邮箱、测试 SMTP

## 📊 和传统登录方案对比

| 对比项 | 传统密码登录 | AutoMailLogin |
|---|---:|---:|
| 邮箱注册验证 | ❌ | ✅ |
| 忘记密码找回 | ⚠️ 通常较弱 | ✅ |
| 邮件二次验证（2FA） | ❌ | ✅ |
| 登录失败锁定 | ⚠️ 不一定有 | ✅ |
| GUI 引导 | ❌ | ✅ |
| 审计日志 | ❌ | ✅ |
| SQLite / MySQL | ⚠️ 视实现而定 | ✅ |
| 可编辑邮件模板 | ❌ | ✅ |
| HTML 邮件支持 | ❌ | ✅ |

## 👤 玩家怎么使用？

### 首次注册

1. 使用 `/mailregister <email>` 绑定邮箱
2. 收到验证码后，输入 `/mailcode <code>`
3. 输入 `/setpassword <password> <confirm>` 设置密码
4. 完成注册并进入已认证状态

### 正常登录

1. 输入 `/login <password>`
2. 如果触发二次验证，再输入 `/mail2fa <code>`

### 忘记密码

1. 输入 `/forgotpassword <email>`
2. 收到验证码后输入 `/resetpassword <code> <newPassword> <confirm>`

### GUI 菜单方式

玩家也可以直接输入：

- `/automaillogin menu`

然后通过菜单点击对应操作，按聊天提示继续输入内容。

## 🧩 玩家有哪些保护机制？

- 未认证状态下，默认会限制：
  - 移动
  - 聊天
  - 交互
  - 背包操作
  - 丢弃物品
  - 拾取物品
  - 非白名单命令
- 登录失败次数可累计，达到阈值后临时锁定
- 验证码发送有冷却时间
- 二次验证支持信任期
- 验证码状态与邮箱验证状态已持久化，减少服务端重启导致流程丢失

## 🛠️ 管理员能做什么？

### 管理员命令

- `/automaillogin admin status <player>`：查看玩家认证状态
- `/automaillogin admin logs <player>`：查看最近审计日志
- `/automaillogin admin force2fa <player>`：强制玩家下次登录触发 2FA
- `/automaillogin admin unbindmail <player>`：解绑玩家邮箱
- `/automaillogin admin resetauth <player>`：重置玩家认证状态
- `/automaillogin admin testsmtp <email>`：按当前 SMTP 配置发送测试邮件

### 管理员可干预的内容

- 强制触发二次验证
- 重置玩家当前认证状态
- 清除绑定邮箱
- 查看最近认证日志
- 查看邮箱验证状态、待处理验证码状态、最近登录情况等

## 🔑 权限节点

| 权限节点 | 默认值 | 说明 |
|---|---:|---|
| `automaillogin.admin` | `op` | 允许使用全部管理员命令 |

## ⚙️ 核心配置项

| 配置项 | 作用 | 示例 |
|---|---|---|
| `database.type` | 选择数据库类型 | `sqlite` / `mysql` |
| `mail.mode` | 邮件模式 | `mock` / `smtp` |
| `mail.smtp.host` | SMTP 服务器地址 | `smtp.example.com` |
| `mail.server-name` | 邮件模板里的服务器名 | `My Server` |
| `mail.support-email` | 邮件模板里的支持邮箱 | `support@example.com` |
| `mail.template-dir` | 邮件模板目录 | `templates` |
| `security.second-factor.mode` | 二次验证触发模式 | `on_new_ip` |
| `security.max-login-attempts` | 最大登录失败次数 | `5` |
| `security.lock-seconds` | 锁定时长 | `300` |

## 📬 邮件模板能力

`AutoMailLogin` 现在支持可编辑邮件模板系统。

### 模板特点

- 支持 **文本模板** + **HTML 模板**
- 模板会自动生成到插件数据目录
- 服主可以直接编辑模板文件
- 支持 `${var}` 格式变量替换

### 模板目录

默认落地到：

- `plugins/AutoMailLogin/templates/`

### 当前模板文件

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

### 可用变量

- `${code}`
- `${player}`
- `${email}`
- `${server_name}`
- `${expire_seconds}`
- `${support_email}`

## 🎯 适合哪些服务器？

- 希望提升账号安全性的生存服 / 公益服
- 需要密码找回和 2FA 的正式服务器
- 希望从 `SQLite` 平滑升级到 `MySQL` 的项目
- 需要审计日志与管理员认证控制能力的服主团队
- 需要自定义邮件模板和品牌化通知的服务器

## ✅ 总结

如果你想要的不是“一个只能输密码的老式登录插件”，而是一个更接近正式产品思路、同时又能真正落地到 `Paper` 服务器中的邮箱认证方案，`AutoMailLogin` 就是为这个场景做的。
