# API 接口说明

基础地址：`http://公网域名:API_PORT/api/v1`。除健康检查外都需要 `Authorization: Bearer API_KEY`。

统一错误格式：

```json
{"error":"unauthorized","message":"Bearer authentication is required"}
```

## 健康与状态

- `GET /health`：无需认证，返回模组是否运行。
- `GET /status`：Minecraft/Fabric 版本、运行时间、内存、在线人数、最大人数、运行状态，以及可用时的 TPS/MSPT。
- `GET /players`：在线玩家的名称、UUID、维度、坐标、血量、饥饿、游戏模式、延迟和等级。

## 指令与管理

- `POST /command`，JSON `{ "command": "say hello" }`：执行任意服务端控制台指令。
- `POST /announce`，JSON `{ "message": "公告内容" }`：向全服发送公告。
- `POST /players/{name}/kick`
- `POST /players/{name}/ban`
- `POST /players/{name}/pardon`
- `POST /players/{name}/whitelist`、`DELETE /players/{name}/whitelist`
- `POST /players/{name}/op`、`DELETE /players/{name}/op`
- `POST /players/{name}/message`，JSON `{ "message": "你好" }`

玩家管理接口使用 Minecraft 的同名服务器指令执行，仍受服务器本身的玩家名和权限规则约束。`/command` 不使用指令白名单，API Key 正确即可执行控制台可执行的任意指令，包括 `stop`。

## 日志、事件和游戏规则

- `GET /logs`：最近的内存控制台日志。
- `GET /events`：SSE 长连接，事件类型为 `join`、`quit`、`chat`、`death`、`command`、`log`；连接会发送心跳。
- `GET /gamerules`：读取当前游戏规则快照。
- `PUT /gamerules/{name}`，JSON `{ "value": "true" }`：修改指定游戏规则。

事件连接断开后会自动清理订阅。服务器停止时 API、SSE、定时器和 HTTP 工作线程都会关闭。

## 线程与超时

请求处理在线程池中进行，所有 Minecraft 状态访问和指令执行提交给 Minecraft 服务端线程。超过 `requestTimeoutMillis` 会返回错误，不会在 HTTP 线程直接修改玩家或世界对象。
