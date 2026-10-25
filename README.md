# Minecraft Server API Fabric Mod

这是一个只安装在 Fabric 服务端的 HTTP API 模组，目标是 Minecraft Java Edition 26.3、Fabric Loader 0.19.5+、Fabric API 0.160.5+ 和 Java 25。客户端不需要安装模组。

## 快速安装

1. 使用 Java 25 启动 Fabric 26.3 服务端一次，确认 Fabric Loader 与 Fabric API 已安装。
2. 将 `serverapi-fabric-1.0.0.jar` 放进服务端的 `mods` 文件夹。
3. 启动服务器。模组会在 `config/serverapi.json` 生成配置和随机 API Key，并在控制台只显示这次首次生成的 Key。
4. 默认监听 `127.0.0.1:8080`。需要公网访问时，把 `bindAddress` 改为 `0.0.0.0` 或指定网卡地址，并确保主机/服务商防火墙放行独立 API 端口。
5. API 端口不能等于 Minecraft 游戏端口；冲突时 API 会拒绝启动，Minecraft 服务器仍继续运行。

完整接口、配置和 curl 示例见 `API.md`、`INSTALL.md`、`CURL.md`。

## API Key

Key 保存在 `config/serverapi.json` 的 `apiKey` 字段，不写死在代码中。请求使用：

```text
Authorization: Bearer API_KEY
```

在服务器控制台执行 `/serverapi token generate` 会重新生成 Key，并只通过命令反馈显示新 Key。旧 Key 会立即失效。

## 构建

使用 Java 25 执行：

```text
./gradlew clean test build
```

Windows：

```text
gradlew.bat clean test build
```

最终可安装 JAR 位于 `Packages`。
