package handler

import (
	"context"
	"fmt"
	"log"

	"strings"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"highstream/live-service/internal/store"
	livev1 "highstream/live-service/pb/live/v1"
	userv1 "highstream/live-service/pb/user/v1"
)

// LiveHandler 实现 LiveService，承载房间相关的业务逻辑
type LiveHandler struct {
	livev1.UnimplementedLiveServiceServer
	store   *store.RoomStore
	userCli userv1.UserServiceClient
}

// NewLiveHandler 组装 handler，
// 注入存储与 user-service 客户端
func NewLiveHandler(roomStore *store.RoomStore, userCli userv1.UserServiceClient) *LiveHandler {
	return &LiveHandler{store: roomStore, userCli: userCli}
}

// CreateRoom 校验参数后创建房间
func (h *LiveHandler) CreateRoom(ctx context.Context, req *livev1.CreateRoomRequest) (*livev1.CreateRoomResponse, error) {
	if req.GetOwnerId() <= 0 {
		log.Fatalln()
		return nil, status.Error(codes.InvalidArgument, "ownerId required")
	}
	if strings.TrimSpace(req.GetTitle()) == "" {
		return nil, status.Error(codes.InvalidArgument, "title required")
	}
	room, err := h.store.Create(ctx, req.GetOwnerId(), strings.TrimSpace(req.GetTitle()))
	if err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}
	return &livev1.CreateRoomResponse{Room: room}, nil
}

// ListRooms 分页查询房间，页码与页大小越界时回落到默认值
func (h *LiveHandler) ListRooms(ctx context.Context, req *livev1.ListRoomsRequest) (*livev1.ListRoomsResponse, error) {
	page := req.GetPage()
	if page < 1 {
		page = 1
	}
	size := req.GetPageSize()
	if size < 1 || size > 100 {
		size = 10
	}
	rooms, total, err := h.store.List(ctx, int(page), int(size))
	if err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}
	return &livev1.ListRoomsResponse{Rooms: rooms, Total: total}, nil
}

// StartLive 通过 gRPC 回调
// user-service 校验主播存在，体现跨语言双向调用
func (h *LiveHandler) StartLive(ctx context.Context, req *livev1.StartLiveRequest) (*livev1.StartLiveResponse, error) {
	room, err := h.store.Get(ctx, req.GetRoomId())
	if err != nil {
		return nil, toStatus(err)
	}

	if _, err := h.userCli.GetUser(ctx, &userv1.GetUserRequest{Id: req.GetOwnerId()}); err != nil {
		// 直接透传 user-service 的 gRPC 错误码（例如 NOT_FOUND）
		return nil, err
	}
	if room.OwnerId != req.GetOwnerId() {
		return nil, status.Error(codes.PermissionDenied, "not room owner")
	}
	if room.Status == store.StatusLiving {
		return nil, status.Error(codes.FailedPrecondition, "room already living")
	}
	playURL := fmt.Sprintf("rtmp://mock.local/live/%d", room.Id)
	updated, err := h.store.SetStatus(ctx, room.Id, store.StatusLiving, playURL)
	if err != nil {
		return nil, toStatus(err)
	}
	return &livev1.StartLiveResponse{Room: updated}, nil
}

// StopLive 将房间置为已结束，并清空播放地址
func (h *LiveHandler) StopLive(ctx context.Context, req *livev1.StopLiveRequest) (*livev1.StopLiveResponse, error) {
	room, err := h.store.Get(ctx, req.GetRoomId())
	if err != nil {
		return nil, toStatus(err)
	}
	updated, err := h.store.SetStatus(ctx, room.Id, store.StatusEnded, "")
	if err != nil {
		return nil, toStatus(err)
	}
	return &livev1.StopLiveResponse{Room: updated}, nil
}

// toStatus 把存储层的领域错误转换成 gRPC 状态码
func toStatus(err error) error {
	switch err {
	case store.ErrRoomNotFound:
		return status.Error(codes.NotFound, err.Error())
	case store.ErrNotOwner:
		return status.Error(codes.PermissionDenied, err.Error())
	default:
		return status.Error(codes.Internal, err.Error())
	}
}
