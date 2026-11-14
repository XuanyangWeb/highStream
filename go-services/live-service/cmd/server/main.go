package main

import (
	"database/sql"
	"log"
	"net"
	"os"
	"time"

	_ "github.com/go-sql-driver/mysql"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"

	"highstream/live-service/internal/grpcclient"
	"highstream/live-service/internal/handler"
	"highstream/live-service/internal/store"
	livev1 "highstream/live-service/pb/live/v1"
)

// env 读取环境变量，未设置时使用默认值
func env(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

// 启动入口：连接数据库、创建到 user-service 的 gRPC 客户端，并监听直播服务端口
func main() {
	dsn := env("LIVE_DB_DSN", "root:root@tcp(127.0.0.1:13306)/highstream?parseTime=true&loc=UTC")
	db, err := sql.Open("mysql", dsn)
	if err != nil {
		log.Fatalf("open db: %v", err)
	}
	db.SetMaxOpenConns(16)
	db.SetConnMaxLifetime(5 * time.Minute)
	if err := db.Ping(); err != nil {
		log.Fatalf("ping db: %v", err)
	}

	userClient, conn, err := grpcclient.NewUserClient(env("USER_GRPC_ADDR", "127.0.0.1:9091"))
	if err != nil {
		log.Fatalf("connect user-service: %v", err)
	}
	defer conn.Close()

	lis, err := net.Listen("tcp", ":"+env("LIVE_PORT", "9090"))
	if err != nil {
		log.Fatalf("listen: %v", err)
	}

	server := grpc.NewServer()
	livev1.RegisterLiveServiceServer(server, handler.NewLiveHandler(store.NewRoomStore(db), userClient))
	reflection.Register(server) // 方便 grpcurl 调试

	log.Printf("live-service listening on %s", lis.Addr())
	if err := server.Serve(lis); err != nil {
		log.Fatalf("serve: %v", err)
	}
}
