# 虚拟 IoT 设备模拟器

不需要任何硬件。模拟器通过 MQTT 发布温湿度、AGV 状态与 RFID 扫描事件，后端订阅后写入
`device_telemetry` 表，设备监控页就会实时更新。

## 1. 安装依赖

```bash
cd simulator
pip install -r requirements.txt
```

## 2. 准备 MQTT Broker

任选一种：

- **Mosquitto（轻量）**：安装后默认监听 `localhost:1883`。
- **EMQX（带管理台）**：Docker 或安装包，默认 `1883`。
- **amqtt（纯 Python，免安装）**：`pip install amqtt`，然后在本目录运行
  `amqtt -c amqtt-broker.yaml`。配置已允许匿名订阅 `#`，适合本地联调。
- **临时测试**：没有 broker 时先用 `--dry-run` 看消息格式。

## 3. 启动模拟器

```bash
# 只打印消息，不连接 broker
python simulator.py --dry-run

# 连接本机 broker
python simulator.py --host localhost --port 1883

# 场景：热浪（温度 +4°C）
python simulator.py --scenario heatwave

# 场景：AGV 故障（AGV-04 离线）
python simulator.py --scenario agv-fault
```

## 4. 让后端接收 MQTT

后端默认关闭 MQTT，避免没有 broker 时启动报错。启动时打开：

Windows PowerShell：

```powershell
$env:MQTT_ENABLED="true"
$env:MQTT_BROKER_URL="tcp://localhost:1883"
cd backend
.\mvnw spring-boot:run
```

Git Bash：

```bash
cd backend
MQTT_ENABLED=true MQTT_BROKER_URL=tcp://localhost:1883 ./mvnw spring-boot:run
```

然后打开前端「设备监控」，温度/湿度曲线会出现新的遥测点；设备表的 `last_heartbeat` 也会更新。

> 已用 `amqtt` + Python 模拟器完成端到端验证：模拟器发布 → broker → 后端订阅 →
> `device_telemetry` 写入 → 设备页出现新点。若使用自建 broker，请确保允许订阅
> `wms/sz01/#`，否则会收到 Suback 0x80。

## 5. Topic 与 Payload

| Topic | Payload 示例 |
|---|---|
| `wms/sz01/sensor/TH-A01/telemetry` | `{"deviceCode":"TH-A01","metric":"temp","value":22.6,"unit":"°C"}` |
| `wms/sz01/sensor/TH-A01/telemetry` | `{"deviceCode":"TH-A01","metric":"humidity","value":48.0,"unit":"%"}` |
| `wms/sz01/agv/AGV-01/state` | `{"deviceCode":"AGV-01","metric":"battery","value":72,"unit":"%","state":"WORKING"}` |
| `wms/sz01/rfid/RFID-A01/scan` | `{"deviceCode":"RFID-A01","metric":"scan_count","value":12,"unit":"次"}` |

- QoS 1。
- `state` 可选，AGV 会把它写回 `device.status`。
- 未来换成 ESP32/DHT22，只要发布同样格式即可，后端无需修改。

## 6. 参数

| 参数 | 默认 | 说明 |
|---|---|---|
| `--host` | `localhost` | broker 地址 |
| `--port` | `1883` | broker 端口 |
| `--interval` | `5` | 发布间隔（秒） |
| `--scenario` | `normal` | `normal` / `heatwave` / `agv-fault` / `rfid-burst` |
| `--dry-run` | 关 | 只打印，不连接 |
| `--config` | `devices.yaml` | 设备清单 |
