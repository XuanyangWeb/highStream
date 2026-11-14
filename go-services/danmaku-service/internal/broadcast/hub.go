package broadcast

import (
	"sync"

	danmakuv1 "highstream/danmaku-service/pb/danmaku/v1"
)

// Hub 进程内广播：
// 每个订阅者一个带缓冲 channel，慢客户端丢最旧消息
type Hub struct {
	mu    sync.RWMutex
	rooms map[int64]map[chan *danmakuv1.Danmaku]struct{}
}

// NewHub 创建进程内广播中心
func NewHub() *Hub {
	return &Hub{rooms: make(map[int64]map[chan *danmakuv1.Danmaku]struct{})}
}

// Subscribe 为指定房间注册一个订阅 channel
func (h *Hub) Subscribe(roomID int64) chan *danmakuv1.Danmaku {
	ch := make(chan *danmakuv1.Danmaku, 64)
	h.mu.Lock()
	defer h.mu.Unlock()
	if h.rooms[roomID] == nil {
		h.rooms[roomID] = make(map[chan *danmakuv1.Danmaku]struct{})
	}
	h.rooms[roomID][ch] = struct{}{}
	return ch
}

// Unsubscribe 注销订阅 channel 并关闭它
func (h *Hub) Unsubscribe(roomID int64, ch chan *danmakuv1.Danmaku) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if subs, ok := h.rooms[roomID]; ok {
		if _, exists := subs[ch]; exists {
			delete(subs, ch)
			close(ch)
		}
		if len(subs) == 0 {
			delete(h.rooms, roomID)
		}
	}
}

// Publish 把弹幕推给该房间所有订阅者，不阻塞调用方
func (h *Hub) Publish(d *danmakuv1.Danmaku) {
	h.mu.RLock()
	defer h.mu.RUnlock()
	for ch := range h.rooms[d.RoomId] {
		select {
		case ch <- d:
		default:
			// channel 满则丢弃最旧的一条，避免阻塞广播
			select {
			case <-ch:
			default:
			}
			select {
			case ch <- d:
			default:
			}
		}
	}
}
