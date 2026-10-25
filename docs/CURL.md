# curl 示例

替换以下变量：

```bash
BASE="http://example.com:8080/api/v1"
KEY="YOUR_API_KEY"
```

健康检查无需 Key：

```bash
curl "$BASE/health"
```

查询状态和玩家：

```bash
curl -H "Authorization: Bearer $KEY" "$BASE/status"
curl -H "Authorization: Bearer $KEY" "$BASE/players"
```

执行任意指令和公告：

```bash
curl -X POST "$BASE/command" \
  -H "Authorization: Bearer $KEY" \
  -H "Content-Type: application/json" \
  -d '{"command":"say Hello from API"}'

curl -X POST "$BASE/announce" \
  -H "Authorization: Bearer $KEY" \
  -H "Content-Type: application/json" \
  -d '{"message":"服务器将在 5 分钟后维护"}'
```

玩家管理：

```bash
curl -X POST "$BASE/players/Steve/kick" -H "Authorization: Bearer $KEY"
curl -X POST "$BASE/players/Steve/ban" -H "Authorization: Bearer $KEY"
curl -X POST "$BASE/players/Steve/pardon" -H "Authorization: Bearer $KEY"
curl -X POST "$BASE/players/Steve/whitelist" -H "Authorization: Bearer $KEY"
curl -X DELETE "$BASE/players/Steve/whitelist" -H "Authorization: Bearer $KEY"
curl -X POST "$BASE/players/Steve/op" -H "Authorization: Bearer $KEY"
curl -X DELETE "$BASE/players/Steve/op" -H "Authorization: Bearer $KEY"

curl -X POST "$BASE/players/Steve/message" \
  -H "Authorization: Bearer $KEY" \
  -H "Content-Type: application/json" \
  -d '{"message":"你好，欢迎回来"}'
```

日志、游戏规则和 SSE：

```bash
curl -H "Authorization: Bearer $KEY" "$BASE/logs"
curl -H "Authorization: Bearer $KEY" "$BASE/gamerules"
curl -N -H "Authorization: Bearer $KEY" "$BASE/events"

curl -X PUT "$BASE/gamerules/keepInventory" \
  -H "Authorization: Bearer $KEY" \
  -H "Content-Type: application/json" \
  -d '{"value":"true"}'
```

HTTPS 时把 `http://` 换成 `https://`；如果使用自签名证书，测试时可临时增加 `-k`，生产环境应使用受信任证书。
