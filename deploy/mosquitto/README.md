# Mosquitto 部署说明

## 首次部署（生成口令文件）

口令文件包含：
1. `zhiliao-server` —— 后端服务账号（密码来自 `.env` 的 `MQTT_PASSWORD`）
2. 每个老年端设备一条 `elder_xxx` 账号（由 elder 表驱动）

```bash
# 在仓库根目录执行（需要 docker）
bash scripts/generate-mqtt-passwd.sh
```

脚本行为：
- 读取 `.env` 的 `MQTT_USERNAME` / `MQTT_PASSWORD` 生成后端账号
- 连接 MySQL 查询 `elder` 表全部 elderId，逐个生成设备账号（密码 = HMAC(deviceId, XIAOZHI_DEVICE_AUTH_SECRET) 截断，与后端 `MqttCredentialService` 同算法）
- 输出 `deploy/mosquitto/passwd`（容器挂载用，**已 gitignore，勿提交**）

## 日常运维

```bash
# 新增老人后刷新设备口令（重跑脚本即可，增量追加）
bash scripts/generate-mqtt-passwd.sh

# 修改口令后让 mosquitto 重载（免重启）
docker exec xiaozhi-mosquitto mosquitto_ctrl 2>/dev/null || \
docker exec xiaozhi-mosquitto kill -HUP 1
```

## MQTTX 联调（本机）

- Host: `127.0.0.1`，Port: `1883`（compose 只映射宿主机回环）
- Username/Password: 用 `elder` 表某设备账号，或临时给 MQTTX 生成一个测试账号
- 订阅 `zhiliao/v1/#` 可观察全部主题流量
