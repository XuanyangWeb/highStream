#!/usr/bin/env bash
# 生成 Go 侧 protobuf 与 gRPC 代码（Java 侧由 Maven 插件生成）
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

mkdir -p go-services/live-service/pb go-services/danmaku-service/pb

protoc -I proto \
  --go_out=go-services/live-service/pb --go_opt=paths=source_relative \
  --go-grpc_out=go-services/live-service/pb --go-grpc_opt=paths=source_relative \
  proto/live/v1/live.proto proto/user/v1/user.proto

protoc -I proto \
  --go_out=go-services/danmaku-service/pb --go_opt=paths=source_relative \
  --go-grpc_out=go-services/danmaku-service/pb --go-grpc_opt=paths=source_relative \
  proto/danmaku/v1/danmaku.proto

echo "proto generated"
