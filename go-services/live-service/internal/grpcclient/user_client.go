package grpcclient

import (
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	userv1 "highstream/live-service/pb/user/v1"
)

// NewUserClient 创建到 user-service 的 gRPC 客户端（明文连接，仅用于本地学习环境）
func NewUserClient(addr string) (userv1.UserServiceClient, *grpc.ClientConn, error) {
	conn, err := grpc.NewClient(addr,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		// 启动期连接未就绪时等待，而不是直接 UNAVAILABLE
		grpc.WithDefaultCallOptions(grpc.WaitForReady(true)))
	if err != nil {
		return nil, nil, err
	}
	return userv1.NewUserServiceClient(conn), conn, nil
}
