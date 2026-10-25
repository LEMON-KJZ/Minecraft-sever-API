# 安装与配置

## 运行环境

- Minecraft Java Edition 26.3 服务端
- Fabric Loader 0.19.5 或更高
- Fabric API 0.160.5+26.3 或更高
- Java 25

## 配置文件

首次服务端启动后编辑 `config/serverapi.json`：

| 字段 | 默认值 | 说明 |
|---|---|---|
| `bindAddress` | `127.0.0.1` | 监听地址；公网监听可填 `0.0.0.0` |
| `port` | `8080` | API 端口，必须和游戏端口分开 |
| `apiKey` | 自动生成 | Bearer Key；留空只会在首次启动生成 |
| `tlsEnabled` | `false` | 可选 HTTPS |
| `keystorePath` | 空 | PKCS12/JKS 证书路径 |
| `keystorePassword` | 空 | 证书密码 |
| `rateLimitPerMinute` | `120` | 每客户端每分钟请求数 |
| `logBufferSize` | `500` | 内存中保留的日志条数 |
| `requestTimeoutMillis` | `5000` | 等待 Minecraft 主线程任务的最长时间 |

修改监听地址或端口后重启服务端。模组不会绕过防火墙或服务商端口限制。

## HTTPS

将 `tlsEnabled` 设为 `true`，并填写 `.p12`/`.pfx` 或 `.jks` 文件路径和密码。证书文件应由服务器管理员创建并限制文件权限。HTTP 和 HTTPS 都使用同一 Bearer Key 认证。

## 重新生成 Key

在服务器控制台执行：

```text
/serverapi token generate
```

新 Key 会显示在这条命令的反馈中；旧 Key 立即失效。不要把 Key 提交到互联网上。
