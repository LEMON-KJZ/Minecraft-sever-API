# 模组使用教程

## 1. 安装前准备

准备以下环境：

- Minecraft Java Edition 26.3 服务端
- Fabric Loader 0.19.5+
- Fabric API 0.160.5+26.3
- Java 25

客户端不需要安装这个模组。

## 2. 安装 JAR

把 `serverapi-fabric-26.3-1.0.0.jar` 放进服务端的 `mods` 文件夹，然后启动服务器。

第一次启动后会生成：

```text
config/serverapi.json
```

控制台会显示一次类似下面的提示：

```text
Server API key generated once during first startup: xxxxxxxxx
```

立即保存这个 Key。以后手机 App 连接需要：

- 公网域名
- API 端口
- API Key

## 3. 配置监听地址和端口

默认配置：

```json
{
  "bindAddress": "127.0.0.1",
  "port": 8080,
  "apiKey": "",
  "tlsEnabled": false,
  "keystorePath": "",
  "keystorePassword": "",
  "rateLimitPerMinute": 120,
  "logBufferSize": 500,
  "requestTimeoutMillis": 5000
}
```

本机测试使用 `127.0.0.1`。公网访问改成：

```json
"bindAddress": "0.0.0.0",
"port": 8080
```

API 端口必须和 `server.properties` 中的 `server-port` 不同。例如游戏端口是 `25565`，API 可以使用 `8080`。

修改配置后重启服务器。还要在云服务器安全组和操作系统防火墙中放行 API 端口。

## 4. 第一次测试

```bash
curl http://127.0.0.1:8080/api/v1/health
```

预期结果：

```json
{"status":"ok"}
```

## 5. 查询服务器状态

```bash
curl -H "Authorization: Bearer API_KEY" \
  http://127.0.0.1:8080/api/v1/status
```

玩家列表：

```bash
curl -H "Authorization: Bearer API_KEY" \
  http://127.0.0.1:8080/api/v1/players
```

## 6. 执行服务器指令

```bash
curl -X POST http://127.0.0.1:8080/api/v1/command \
  -H "Authorization: Bearer API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"command":"say API connected"}'
```

它使用 Minecraft 服务端命令分发器执行，因此可以执行 `say`、`list`、`kick`、`ban`、`give`、`tp`、`gamerule`、`stop` 等控制台支持的指令。

## 7. 玩家管理

```bash
curl -X POST http://127.0.0.1:8080/api/v1/players/Steve/kick \
  -H "Authorization: Bearer API_KEY"

curl -X POST http://127.0.0.1:8080/api/v1/players/Steve/message \
  -H "Authorization: Bearer API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"message":"你好"}'
```

白名单和 OP：

```bash
curl -X POST http://127.0.0.1:8080/api/v1/players/Steve/whitelist \
  -H "Authorization: Bearer API_KEY"

curl -X DELETE http://127.0.0.1:8080/api/v1/players/Steve/op \
  -H "Authorization: Bearer API_KEY"
```

## 8. 公告、游戏规则、日志和实时事件

公告：

```bash
curl -X POST http://127.0.0.1:8080/api/v1/announce \
  -H "Authorization: Bearer API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"message":"服务器将在 5 分钟后维护"}'
```

游戏规则：

```bash
curl -H "Authorization: Bearer API_KEY" \
  http://127.0.0.1:8080/api/v1/gamerules

curl -X PUT http://127.0.0.1:8080/api/v1/gamerules/keepInventory \
  -H "Authorization: Bearer API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"value":"true"}'
```

最近日志：

```bash
curl -H "Authorization: Bearer API_KEY" \
  http://127.0.0.1:8080/api/v1/logs
```

实时事件：

```bash
curl -N -H "Authorization: Bearer API_KEY" \
  http://127.0.0.1:8080/api/v1/events
```

事件类型包括玩家加入、退出、聊天、死亡、指令和控制台日志。

## 9. 重新生成 API Key

在 Minecraft 服务端控制台执行：

```text
/serverapi token generate
```

新 Key 会显示在命令反馈中，旧 Key 立即失效。重新生成后必须同步更新安卓 App 配置。

## 10. HTTPS

准备 PKCS12/PFX 或 JKS 证书，然后修改：

```json
"tlsEnabled": true,
"keystorePath": "cert/server.p12",
"keystorePassword": "证书密码"
```

重启后使用：

```bash
curl -k -H "Authorization: Bearer API_KEY" \
  https://example.com:8080/api/v1/status
```

生产环境建议使用受信任证书，避免长期使用 `-k`。

## 11. 安全建议

- 默认只监听 `127.0.0.1`。
- 公网部署时优先启用 HTTPS。
- 不要把 API Key 发到群聊、提交到 GitHub 或写进 App 源代码。
- 不要把真实 `config/serverapi.json` 上传到 GitHub。
- `/serverapi token generate` 只给服务器管理员使用。
- API Key 正确时 `/api/v1/command` 具备完整控制台权限，尤其要谨慎处理 `stop`、`op`、`ban` 等指令。
