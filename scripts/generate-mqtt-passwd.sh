#!/usr/bin/env bash
# ============================================================
# 生成 Mosquitto 口令文件 deploy/mosquitto/passwd
# 1. 后端服务账号（.env 的 MQTT_USERNAME/MQTT_PASSWORD）
# 2. 每个老人一台设备账号（elder 表驱动）
#    设备密码 = HMAC_SHA256(deviceId, XIAOZHI_DEVICE_AUTH_SECRET) 前 24 位
#    与后端 MqttCredentialService 同算法，两端一致
# ============================================================
set -euo pipefail

cd "$(dirname "$0")/.."

# 读 .env（存在才加载，变量可被外部环境覆盖）
if [ -f .env ]; then
  set -a; source .env; set +a
fi

MQTT_USERNAME="${MQTT_USERNAME:-zhiliao-server}"
MQTT_PASSWORD="${MQTT_PASSWORD:-}"
DEVICE_AUTH_SECRET="${XIAOZHI_DEVICE_AUTH_SECRET:-}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-xiaozhi-mysql-1}"
MYSQL_USER="${MYSQL_USER:-xiaozhi}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-123456}"
MYSQL_DATABASE="${MYSQL_DATABASE:-xiaozhi}"
OUT="deploy/mosquitto/passwd"

[ -n "$MQTT_PASSWORD" ] || { echo "错误：.env 缺少 MQTT_PASSWORD"; exit 1; }
[ -n "$DEVICE_AUTH_SECRET" ] || { echo "错误：.env 缺少 XIAOZHI_DEVICE_AUTH_SECRET（设备密码派生依赖它）"; exit 1; }

TMP="$(mktemp)"
trap 'rm -f "$TMP"' EXIT
# mosquitto_passwd 需写真实文件：挂载宿主机临时目录进容器
TMPDIR_HOST="$(mktemp -d)"
trap 'rm -f "$TMP"; rm -rf "$TMPDIR_HOST"' EXIT

# 1) 后端服务账号
docker run --rm -v "$TMPDIR_HOST:/work" eclipse-mosquitto:2 \
  mosquitto_passwd -c -b /work/passwd "$MQTT_USERNAME" "$MQTT_PASSWORD"

# 2) 老年端设备账号（逐个生成）
ELDERS=$(docker exec "$MYSQL_CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -B \
  -e "SELECT elderId, deviceId FROM ${MYSQL_DATABASE}.elder WHERE state='1' AND deviceId IS NOT NULL;" \
  2>/dev/null | grep -v "Using a password")

while IFS=$'\t' read -r elderId deviceId; do
  [ -n "$deviceId" ] || continue
  # 设备密码：HMAC(deviceId, secret) 前 24 位十六进制
  PW=$(printf '%s' "$deviceId" | openssl dgst -sha256 -hmac "$DEVICE_AUTH_SECRET" \
       -hex | awk '{print $NF}' | cut -c1-24)
  # 用户名必须用 elderId：与服务端 careMqttConfig 下发的 username 及 ACL 的 user 一致
  docker run --rm -v "$TMPDIR_HOST:/work" eclipse-mosquitto:2 \
    mosquitto_passwd -b /work/passwd "$elderId" "$PW"
  echo "  + 设备账号 $elderId (device: $deviceId)"
done <<< "$ELDERS"

mkdir -p deploy/mosquitto
cp "$TMPDIR_HOST/passwd" "$OUT"
echo "已生成 $OUT（共 $(grep -c ':' "$OUT") 个账号）"
echo "生效：docker restart \${MYSQL_CONTAINER%mysql-1}mosquitto  或  docker exec xiaozhi-mosquitto kill -HUP 1"
