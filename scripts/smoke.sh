#!/usr/bin/env bash
# 端到端冒烟：建用户 -> 建房 -> 开播 -> 发弹幕 -> SSE -> 下播
set -euo pipefail

BASE="${BASE:-http://127.0.0.1:8080/api}"
fail() { echo "FAIL: $1"; exit 1; }

echo "1) 创建用户"
USER=$(curl -sf -X POST "$BASE/users" -H "Content-Type: application/json" \
  -d '{"name":"smoke-user","avatar":""}') || fail "create user"
USER_ID=$(echo "$USER" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "   userId=$USER_ID"

echo "2) 创建房间"
ROOM=$(curl -sf -X POST "$BASE/live/rooms" -H "Content-Type: application/json" \
  -d "{\"ownerId\":$USER_ID,\"title\":\"smoke room\"}") || fail "create room"
ROOM_ID=$(echo "$ROOM" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "   roomId=$ROOM_ID"

echo "3) 开播（Go 服务回调 Java GetUser 校验主播）"
STARTED=$(curl -sf -X POST "$BASE/live/rooms/$ROOM_ID/start" -H "Content-Type: application/json" \
  -d "{\"ownerId\":$USER_ID}") || fail "start live"
echo "$STARTED" | grep -q '"status":1' || fail "status not LIVING: $STARTED"
echo "   $STARTED"

echo "4) 订阅 SSE 并发送弹幕"
SSE_FILE=$(mktemp)
curl -sN --max-time 6 "$BASE/danmaku/sse/$ROOM_ID" >"$SSE_FILE" &
SSE_PID=$!
sleep 1
curl -sf -X POST "$BASE/danmaku" -H "Content-Type: application/json" \
  -d "{\"roomId\":$ROOM_ID,\"userId\":$USER_ID,\"content\":\"smoke-hello\"}" >/dev/null || fail "send danmaku"
wait $SSE_PID || true

grep -q "smoke-hello" "$SSE_FILE" || { cat "$SSE_FILE"; fail "sse did not receive danmaku"; }
echo "   SSE 已收到弹幕"

echo "5) 下播"
STOPPED=$(curl -sf -X POST "$BASE/live/rooms/$ROOM_ID/stop") || fail "stop live"
echo "$STOPPED" | grep -q '"status":2' || fail "status not ENDED: $STOPPED"
echo "   $STOPPED"

echo "SMOKE OK"
