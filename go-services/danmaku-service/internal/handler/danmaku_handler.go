package handler

import (
	"context"
	"strings"
	"time"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"highstream/danmaku-service/internal/broadcast"
	"highstream/danmaku-service/internal/store"
	danmakuv1 "highstream/danmaku-service/pb/danmaku/v1"
)

// DanmakuHandler 实现 DanmakuService，负责发送与订阅弹幕
type DanmakuHandler struct {
	danmakuv1.UnimplementedDanmakuServiceServer
	store *store.DanmakuStore
	hub   *broadcast.Hub
}

// NewDanmakuHandler 组装 handler，注入存储与广播中心
func NewDanmakuHandler(store *store.DanmakuStore, hub *broadcast.Hub) *DanmakuHandler {
	return &DanmakuHandler{store: store, hub: hub}
}

// SendDanmaku 校验参数后写入 Redis，并广播给本进程内的订阅者
func (h *DanmakuHandler) SendDanmaku(ctx context.Context, req *danmakuv1.SendDanmakuRequest) (*danmakuv1.SendDanmakuResponse, error) {
	if req.GetRoomId() <= 0 {
		return nil, status.Error(codes.InvalidArgument, "roomId required")
	}
	if req.GetUserId() <= 0 {
		return nil, status.Error(codes.InvalidArgument, "userId required")
	}
	content := strings.TrimSpace(req.GetContent())
	if content == "" {
		return nil, status.Error(codes.InvalidArgument, "content required")
	}
	d := &danmakuv1.Danmaku{
		RoomId:  req.GetRoomId(),
		UserId:  req.GetUserId(),
		Content: content,
		Ts:      time.Now().UnixMilli(),
	}
	if err := h.store.Append(ctx, d); err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}
	h.hub.Publish(d)
	return &danmakuv1.SendDanmakuResponse{Danmaku: d}, nil
}

// Subscribe 服务端流式订阅：先回放最近弹幕，再持续推送新弹幕
func (h *DanmakuHandler) Subscribe(req *danmakuv1.SubscribeRequest, stream danmakuv1.DanmakuService_SubscribeServer) error {
	if req.GetRoomId() <= 0 {
		return status.Error(codes.InvalidArgument, "roomId required")
	}
	roomID := req.GetRoomId()
	ch := h.hub.Subscribe(roomID)
	defer h.hub.Unsubscribe(roomID, ch)

	// 回放最近弹幕
	recent, err := h.store.Recent(stream.Context(), roomID)
	if err != nil {
		return status.Error(codes.Internal, err.Error())
	}
	for _, d := range recent {
		if err := stream.Send(&danmakuv1.SubscribeResponse{Danmaku: d}); err != nil {
			return err
		}
	}

	for {
		select {
		case <-stream.Context().Done():
			return nil
		case d, ok := <-ch:
			if !ok {
				return nil
			}
			if err := stream.Send(&danmakuv1.SubscribeResponse{Danmaku: d}); err != nil {
				return err
			}
		}
	}
}
