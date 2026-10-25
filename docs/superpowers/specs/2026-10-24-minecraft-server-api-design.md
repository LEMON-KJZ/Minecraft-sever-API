# Minecraft Java 26.3 Fabric Server API 模组设计

日期：2026-10-24

## 意图

制作一个只安装在 Fabric 服务端的 Minecraft Java Edition 26.3 模组，在模组进程内提供可配置的 HTTP API。管理员使用公网域名、独立 API 端口和 API Key，通过 API 查看服务器状态、执行任意服务器指令、管理玩家和读取实时事件；后续安卓 App 只依赖这组 API。

## 目标

- 支持 Fabric Loader 0.19.5+、Fabric API 0.160.5+、Java 25 和 Minecraft 26.3。
- API 与 Minecraft 游戏端口分离，默认监听 127.0.0.1，可配置为 0.0.0.0 或指定地址。
- 首次启动生成随机 API Key，只在控制台显示一次并保存到服务器配置；命令 `/serverapi token generate` 可重新生成。
- 使用 `Authorization: Bearer <API_KEY>` 认证，错误响应统一为 JSON。
- 提供健康、状态、玩家、任意指令、公告、玩家管理、日志、SSE 事件、游戏规则接口。
- 所有 Minecraft API 读写和命令执行通过 Minecraft 服务端线程完成，HTTP 工作线程不直接修改游戏状态。
- 交付完整 Gradle 工程、可放入 mods 的 JAR、配置示例、安装/API/curl 文档，并实际构建验证。

## 非目标与限制

- 不依赖网页面板、反向代理、外部后端或模拟点击。
- 不负责服务器进程关闭后的自动启动、操作系统/主机控制、防火墙或服务商面板。
- 第一版不强制要求 HTTPS；会预留并实现基于 PKCS12/JKS 的可选 TLS 配置，若不影响核心构建稳定性。
- 不提供热更新其他模组。

## 约束

- 认证密钥不能硬编码，也不能在普通日志中重复输出。
- `/api/v1/command` 允许任意控制台可执行指令，不做白名单；通过 Minecraft 的命令分发器执行。
- API 端口启动时必须检查不能与游戏端口相同，冲突时拒绝启动 API 并记录明确错误。
- 公网绑定由配置和主机网络策略决定，模组不绕过防火墙。

## 架构与实现选择

### 方案 A：Java 内置 HTTP Server（采用）

使用 Java 25 的 `com.sun.net.httpserver.HttpServer`（TLS 时使用 `HttpsServer`）和受控线程池。优点是依赖少、JAR 小、适合 Fabric 服务端直接安装。SSE 使用长连接和订阅者集合实现；每个请求的游戏操作封装为主线程任务并通过 `CompletableFuture` 等待结果，带超时和统一错误转换。

### 方案 B：内置 Netty/HTTP 框架

可获得更成熟的路由、SSE、TLS 和连接管理能力，但会增加打包依赖、冲突风险和维护成本；第一版收益不足以抵消复杂度。

### 方案 C：外部控制程序或网页面板

与“模组内置 API、不要依赖网页面板”的约束冲突，因此排除。

## 主要模块

- `ServerApiMod`：Fabric 入口、服务器启动/停止生命周期、事件注册、命令注册。
- `ServerApiConfig`：JSON 配置读写，保存 bind address、API port、API key、TLS、限流、日志大小和自动广播等选项。
- `ApiServer`：HTTP/HTTPS 监听、路由、认证、限流、JSON 序列化和 SSE。
- `MinecraftExecutor`：把需要访问 `MinecraftServer` 的动作提交到服务器线程，统一超时和异常处理。
- `GameStateService`：状态、玩家、维度、游戏规则和扩展信息的快照生成。
- `CommandService`：调用服务端命令分发器执行任意命令，捕获返回码与反馈文本，并发布“指令执行”事件。
- `LogBuffer`：内存环形日志缓冲，记录最近控制台日志；API Key 本身永不写入缓冲。
- `EventBus`：玩家加入/退出、聊天、死亡、指令、日志事件；SSE 客户端断开时清理订阅。
- `BackupService`：调用世界保存并创建简单时间戳备份（若配置开启）。

## API 形状

所有路径前缀为 `/api/v1`。除健康检查外都要求 Bearer Key。

- `GET /health`：运行状态与模组版本。
- `GET /status`：Minecraft/Fabric 版本、运行时间、内存、TPS/MSPT（可用时）、在线人数、最大人数、运行状态。
- `GET /players`：名称、UUID、维度、坐标、血量、饥饿、模式、延迟、等级。
- `POST /command`：JSON `{ "command": "say hello" }`，返回执行码、反馈和耗时。
- `POST /announce`：JSON `{ "message": "..." }`。
- `POST /players/{name}/kick|ban|pardon|whitelist|op`，对应 DELETE 取消 whitelist/op。
- `POST /players/{name}/message`：JSON `{ "message": "..." }`。
- `GET /logs`：最近日志，支持 `limit` 查询参数。
- `GET /events`：SSE，事件类型包含 join、quit、chat、death、command、log。
- `GET /gamerules`、`PUT /gamerules/{name}`：读取/修改游戏规则。

扩展接口会在核心稳定后加入：世界/天气/难度、保存世界、简单备份、模组列表、配置快照、自动广播/定时任务、操作日志。

## 安全与可靠性

- API Key 使用 `SecureRandom` 生成并只在首次生成或明确重新生成命令时向控制台输出一次。
- 使用常量时间比较避免简单时序泄漏；Bearer 头缺失/错误返回 401，不泄露密钥。
- 配置限流按客户端地址维护令牌桶/固定窗口；超限返回 429。
- 所有 JSON 错误包含稳定的 `error`、`message` 字段；内部异常写入服务端日志但不返回堆栈。
- HTTP 工作线程不直接读写玩家对象；快照和命令都在服务器线程执行。
- 服务端关闭时停止监听、结束 SSE、清理线程池和定时任务。

## 验证与交付

- Gradle 运行 `build`，确认 Fabric 模组 JAR 生成。
- 运行单元测试覆盖配置/密钥、认证、路由错误、限流和 MinecraftExecutor；运行构建检查。
- 文档提供安装、配置、首次启动、重新生成 Key、端口冲突和 curl 示例。
- 最终输出路径放入 `outputs/`，包括 JAR 和可复制的配置/文档。
