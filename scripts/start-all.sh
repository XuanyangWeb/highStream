#!/usr/bin/env bash
# 一键启动：基础设施 + 两个 Go 服务 + 两个 Java 服务
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-D:\\installed\\Temurin}"
export GOPROXY="${GOPROXY:-https://goproxy.cn,direct}"
mkdir -p "$ROOT/.run-logs"

echo "==> 启动基础设施"
(cd "$ROOT/deploy" && docker compose up -d)

echo "==> 启动 live-service"
(cd "$ROOT/go-services/live-service" && nohup go run ./cmd/server >"$ROOT/.run-logs/live-service.log" 2>&1 &)

echo "==> 启动 danmaku-service"
(cd "$ROOT/go-services/danmaku-service" && nohup go run ./cmd/server >"$ROOT/.run-logs/danmaku-service.log" 2>&1 &)

echo "==> 启动 user-service"
(cd "$ROOT" && nohup ./mvnw -B -pl java-services/user-service spring-boot:run >"$ROOT/.run-logs/user-service.log" 2>&1 &)

echo "==> 等待 user-service 就绪"
until curl -s -o /dev/null --max-time 2 http://127.0.0.1:8081/actuator/health; do sleep 2; done

echo "==> 启动 gateway"
(cd "$ROOT" && nohup ./mvnw -B -pl java-services/gateway spring-boot:run >"$ROOT/.run-logs/gateway.log" 2>&1 &)

echo "==> 等待 gateway 就绪"
until curl -s -o /dev/null --max-time 2 http://127.0.0.1:8080/actuator/health; do sleep 2; done

echo "全部服务已启动："
echo "  网关        http://127.0.0.1:8080"
echo "  Nacos       http://127.0.0.1:8848/nacos (nacos/nacos)"
echo "  演示页      http://127.0.0.1:8080/room.html"
echo "  日志目录    $ROOT/.run-logs"
