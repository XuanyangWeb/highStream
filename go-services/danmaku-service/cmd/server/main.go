package main

import (
	"context"
	"log"
	"net"
	"os"

	"github.com/redis/go-redis/v9"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"

	"highstream/danmaku-service/internal/broadcast"
	"highstream/danmaku-service/internal/handler"
	"highstream/danmaku-service/internal/store"
	danmakuv1 "highstream/danmaku-service/pb/danmaku/v1"
)

// env 读取环境变量，未设置时使用默认值
func env(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

// 启动入口：连接 Redis，并监听弹幕服务端口
func main() {
	rdb := redis.NewClient(&redis.Options{
		Addr:     env("REDIS_ADDR", "127.0.0.1:16379"),
		Password: env("REDIS_PASSWORD", ""),
	})
	if err := rdb.Ping(context.Background()).Err(); err != nil {
		log.Fatalf("ping redis: %v", err)
	}

	lis, err := net.Listen("tcp", ":"+env("DANMAKU_PORT", "9092"))
	if err != nil {
		log.Fatalf("listen: %v", err)
	}

	server := grpc.NewServer()
	danmakuv1.RegisterDanmakuServiceServer(server,
		handler.NewDanmakuHandler(store.NewDanmakuStore(rdb), broadcast.NewHub()))
	reflection.Register(server)

	log.Printf("danmaku-service listening on %s", lis.Addr())
	if err := server.Serve(lis); err != nil {
		log.Fatalf("serve: %v", err)
	}
}
