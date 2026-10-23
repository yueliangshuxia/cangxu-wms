#!/usr/bin/env python3
"""Virtual warehouse IoT simulator.

Publishes MQTT messages in the same shape as real devices. No hardware required.
The backend subscribes to wms/{warehouse}/# and writes telemetry to device_telemetry.

Examples:
    python simulator.py --dry-run
    python simulator.py --host localhost --port 1883 --scenario normal
    python simulator.py --scenario heatwave
    python simulator.py --scenario agv-fault
"""

import argparse
import json
import math
import random
import time
from pathlib import Path

import yaml

try:
    import paho.mqtt.client as mqtt
except ImportError:  # dry-run can work without paho
    mqtt = None


def parse_args():
    parser = argparse.ArgumentParser(description="Cangxu WMS virtual IoT simulator")
    parser.add_argument("--host", default="localhost", help="MQTT broker host")
    parser.add_argument("--port", type=int, default=1883, help="MQTT broker port")
    parser.add_argument("--warehouse", default=None, help="warehouse code, overrides yaml")
    parser.add_argument("--interval", type=float, default=5.0, help="publish interval in seconds")
    parser.add_argument("--scenario", default="normal",
                        choices=["normal", "heatwave", "agv-fault", "rfid-burst"])
    parser.add_argument("--dry-run", action="store_true", help="print messages instead of publishing")
    parser.add_argument("--config", default=str(Path(__file__).with_name("devices.yaml")))
    return parser.parse_args()


def build_client(host, port):
    if mqtt is None:
        raise SystemExit("paho-mqtt is not installed. Run: pip install -r requirements.txt")
    try:
        client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="cangxu-simulator")
    except AttributeError:
        client = mqtt.Client(client_id="cangxu-simulator")
    client.connect(host, port, 60)
    client.loop_start()
    return client


def publish(client, topic, payload, dry_run):
    body = json.dumps(payload, ensure_ascii=False)
    if dry_run:
        print(f"{topic}  {body}")
        return
    client.publish(topic, body, qos=1)


def main():
    args = parse_args()
    config = yaml.safe_load(Path(args.config).read_text(encoding="utf-8"))
    warehouse = args.warehouse or config.get("warehouse", "sz01")
    sensors = config.get("sensors", [])
    agvs = config.get("agvs", [])
    rfid_readers = config.get("rfid", [])

    client = None if args.dry_run else build_client(args.host, args.port)
    print(f"[simulator] warehouse={warehouse} scenario={args.scenario} "
          f"dry_run={args.dry_run} interval={args.interval}s")
    if args.dry_run:
        print("[simulator] dry-run mode: printing messages only")

    tick = 0
    try:
        while True:
            phase = tick / 12.0 * math.pi * 2
            for sensor in sensors:
                base_temp = float(sensor.get("base_temp", 22.0))
                base_humidity = float(sensor.get("base_humidity", 50))
                heat = 4.0 if args.scenario == "heatwave" else 0.0
                temp = base_temp + heat + 2.0 * math.sin(phase - 1.0) + random.uniform(-0.25, 0.25)
                humidity = base_humidity + 4.0 * math.sin(phase + 0.8) + random.uniform(-0.8, 0.8)
                code = sensor["code"]
                publish(client, f"wms/{warehouse}/sensor/{code}/telemetry",
                        {"deviceCode": code, "metric": "temp",
                         "value": round(temp, 1), "unit": "°C"}, args.dry_run)
                publish(client, f"wms/{warehouse}/sensor/{code}/telemetry",
                        {"deviceCode": code, "metric": "humidity",
                         "value": round(humidity, 1), "unit": "%"}, args.dry_run)

            for agv in agvs:
                code = agv["code"]
                battery = float(agv.get("battery", 80))
                state = agv.get("state", "WORKING")
                if args.scenario == "agv-fault" and code == "AGV-04":
                    state = "OFFLINE"
                elif code == "AGV-07" and battery < 100:
                    state = "CHARGING"
                    battery = min(100, battery + 2.0)
                elif state == "CHARGING":
                    state = "WORKING"
                if state == "WORKING":
                    battery = max(0, battery - 0.3)
                agv["battery"] = round(battery, 1)
                agv["state"] = state
                publish(client, f"wms/{warehouse}/agv/{code}/state",
                        {"deviceCode": code, "metric": "battery",
                         "value": round(battery, 1), "unit": "%", "state": state}, args.dry_run)

            if rfid_readers and (tick % 3 == 0 or args.scenario == "rfid-burst"):
                reader = random.choice(rfid_readers)["code"]
                count = random.randint(6, 18) if args.scenario == "rfid-burst" else random.randint(1, 5)
                publish(client, f"wms/{warehouse}/rfid/{reader}/scan",
                        {"deviceCode": reader, "metric": "scan_count",
                         "value": count, "unit": "次"}, args.dry_run)

            tick += 1
            time.sleep(max(0.5, args.interval))
    except KeyboardInterrupt:
        print("\n[simulator] stopped")
    finally:
        if client is not None:
            client.loop_stop()
            client.disconnect()


if __name__ == "__main__":
    main()
