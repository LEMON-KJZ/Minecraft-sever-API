# Minecraft Server API Fabric Mod

这是一个只安装在 Fabric 服务端的 HTTP API 模组，目标是 Minecraft Java
Edition 26.3、Fabric Loader 0.19.5+、Fabric API 0.160.5+ 和 Java
25。客户端不需要安装模组。

通过 HTTP
API，你可以查询服务器状态、获取在线玩家信息、执行服务端指令、发送公告、管理玩家、读取日志，并订阅实时事件。

## ✨ 功能特性

-   **服务器状态**：查询 Minecraft/Fabric
    版本、运行时间、内存、在线人数、最大人数、运行状态，以及可用时的
    TPS/MSPT。
-   **在线玩家**：获取玩家名称、UUID、维度、坐标、血量、饥饿值、游戏模式、延迟和等级。
-   **指令与公告**：执行服务端控制台指令，或向全服发送公告。
-   **玩家管理**：支持踢出、封禁、解除封禁、白名单、OP 权限和私信操作。
-   **日志与事件**：读取最近的内存控制台日志，并通过 SSE
    订阅玩家加入、退出、聊天、死亡、指令和日志事件。
-   **游戏规则**：读取当前游戏规则快照，并修改指定游戏规则。
-   **API Key 认证**：除健康检查外，API 请求都需要 Bearer API Key。
-   **HTTPS 支持**：可以配置 TLS 证书启用 HTTPS。

## 📋 运行环境

<table>
<thead>
<tr>
<th>组件</th>
<th>版本要求</th>
</tr>
</thead>
<tbody>
<tr>
<td>Minecraft</td>
<td>Java Edition 26.3</td>
</tr>
<tr>
<td>Fabric Loader</td>
<td>0.19.5 或更高</td>
</tr>
<tr>
<td>Fabric API</td>
<td>0.160.5+26.3 或更高</td>
</tr>
<tr>
<td>Java</td>
<td>25</td>
</tr>
</tbody>
</table>

## 📥 快速安装

1.  使用 Java 25 启动 Fabric 26.3 服务端一次，确认 Fabric Loader 和
    Fabric API 已安装。
2.  下载本项目发布的 `serverapi-fabric-26.3-1.0.0.jar`。
3.  将 JAR 文件放进服务端的 `mods` 文件夹。
4.  启动服务器。
5.  首次启动后，模组会在 `config/serverapi.json` 生成配置文件和随机 API
    Key。
6.  按需修改配置并重启服务端。

默认监听地址为
`127.0.0.1:8080`，即默认仅允许本机访问。需要远程访问时，请按实际网络环境修改
`bindAddress`，并正确配置主机或服务商防火墙。API 端口必须与 Minecraft
游戏端口分开。

详细说明见 [INSTALL.md](INSTALL.md)。

## 🔑 API 认证与安全

除健康检查接口外，其他 API 请求都需要提供 Bearer API Key。请求头格式：

``` http
Authorization: Bearer YOUR_API_KEY
```

API Key 保存在服务器的 `config/serverapi.json` 配置文件中。

如需重新生成 Key，可在服务器控制台执行：

``` text
/serverapi token generate
```

新 Key 会显示在这条命令的反馈中，旧 Key 会立即失效。

> **安全提醒：** `/command` 接口允许执行任意服务端控制台指令，包括
> `stop`。请妥善保管 API Key。不要将真实的 `config/serverapi.json`、API
> Key 或证书密码提交到公开仓库；仓库中只应放置不含真实密钥的示例配置。

## 🔌 API 接口概览

基础地址示例：

```text
http://127.0.0.1:8080/api/v1
```

<table>
<thead>
<tr>
<th>方法</th>
<th>接口</th>
<th>说明</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>GET</code></td>
<td><code>/health</code></td>
<td>健康检查，无需认证</td>
</tr>
<tr>
<td><code>GET</code></td>
<td><code>/status</code></td>
<td>查询服务器运行状态、版本、内存和在线人数</td>
</tr>
<tr>
<td><code>GET</code></td>
<td><code>/players</code></td>
<td>获取在线玩家信息</td>
</tr>
<tr>
<td><code>POST</code></td>
<td><code>/command</code></td>
<td>执行服务端控制台指令</td>
</tr>
<tr>
<td><code>POST</code></td>
<td><code>/announce</code></td>
<td>发送全服公告</td>
</tr>
<tr>
<td><code>GET</code></td>
<td><code>/logs</code></td>
<td>查询最近的内存日志</td>
</tr>
<tr>
<td><code>GET</code></td>
<td><code>/events</code></td>
<td>订阅 SSE 实时事件</td>
</tr>
<tr>
<td><code>GET</code></td>
<td><code>/gamerules</code></td>
<td>获取当前游戏规则</td>
</tr>
<tr>
<td><code>PUT</code></td>
<td><code>/gamerules/{name}</code></td>
<td>修改指定游戏规则</td>
</tr>
<tr>
<td><code>POST</code></td>
<td><code>/players/{name}/kick</code></td>
<td>踢出玩家</td>
</tr>
<tr>
<td><code>POST</code></td>
<td><code>/players/{name}/ban</code></td>
<td>封禁玩家</td>
</tr>
<tr>
<td><code>POST</code></td>
<td><code>/players/{name}/pardon</code></td>
<td>解除玩家封禁</td>
</tr>
<tr>
<td><code>POST / DELETE</code></td>
<td><code>/players/{name}/whitelist</code></td>
<td>添加或移除白名单</td>
</tr>
<tr>
<td><code>POST / DELETE</code></td>
<td><code>/players/{name}/op</code></td>
<td>授予或移除 OP 权限</td>
</tr>
<tr>
<td><code>POST</code></td>
<td><code>/players/{name}/message</code></td>
<td>向指定玩家发送私信</td>
</tr>
</tbody>
</table>

除健康检查外，其他接口均需要通过 Bearer API Key 认证。

**相关文档：**

- [API.md](API.md) — 完整接口说明
- [CURL.md](CURL.md) — 请求示例

## ⚙️ 配置

首次启动服务器后，编辑以下配置文件：

```text
config/serverapi.json
```

### 配置项说明

<table>
<thead>
<tr>
<th>配置项</th>
<th>默认值</th>
<th>说明</th>
</tr>
</thead>
<tbody>
<tr>
<td><code>bindAddress</code></td>
<td><code>127.0.0.1</code></td>
<td>API 监听地址，默认仅允许本机访问</td>
</tr>
<tr>
<td><code>port</code></td>
<td><code>8080</code></td>
<td>API 监听端口，必须与 Minecraft 游戏端口分开</td>
</tr>
<tr>
<td><code>apiKey</code></td>
<td>自动生成</td>
<td>Bearer API 身份验证密钥</td>
</tr>
<tr>
<td><code>tlsEnabled</code></td>
<td><code>false</code></td>
<td>是否启用 HTTPS</td>
</tr>
<tr>
<td><code>keystorePath</code></td>
<td>空</td>
<td>PKCS12/JKS 证书文件路径</td>
</tr>
<tr>
<td><code>keystorePassword</code></td>
<td>空</td>
<td>证书密码</td>
</tr>
<tr>
<td><code>rateLimitPerMinute</code></td>
<td><code>120</code></td>
<td>每个客户端每分钟允许的请求数</td>
</tr>
<tr>
<td><code>logBufferSize</code></td>
<td><code>500</code></td>
<td>内存中保留的日志条数</td>
</tr>
<tr>
<td><code>requestTimeoutMillis</code></td>
<td><code>5000</code></td>
<td>等待 Minecraft 主线程任务的最长时间，单位为毫秒</td>
</tr>
</tbody>
</table>

### 修改配置

修改监听地址或端口后，需要重启 Minecraft 服务端。

默认监听地址为 `127.0.0.1`，只能从服务器本机访问 API。如果需要远程访问，请根据实际网络环境修改 `bindAddress`，并正确配置防火墙和端口转发。

**安全提醒：** 不要将真实的 API Key、证书密码或包含敏感信息的 `config/serverapi.json` 上传到公开 GitHub 仓库。

详细配置说明请查看 [INSTALL.md](INSTALL.md)。

## 🛠️ 从源码构建

需要安装 Java 25。

**Windows：**

``` powershell
gradlew.bat clean test build
```

**Linux / macOS：**

``` bash
./gradlew clean test build
```

构建完成后，可安装的 JAR 位于 `build/libs/`。项目中的 `outputs/`
目录可用于存放交付文件。

## 📚 项目文档

-   [API.md](API.md) --- API 接口说明与功能列表
-   [INSTALL.md](INSTALL.md) --- 安装、配置与 HTTPS 指南
-   [CURL.md](CURL.md) --- curl 请求示例

以上文档都放在项目根目录中。

## 📄 License

请参阅仓库中的 [LICENSE](LICENSE) 文件。

------------------------------------------------------------------------

**Minecraft Server API Fabric Mod** --- 通过 HTTP API 管理和监控你的
Minecraft 服务端。
