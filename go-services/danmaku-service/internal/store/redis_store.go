package store

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/redis/go-redis/v9"

	danmakuv1 "highstream/danmaku-service/pb/danmaku/v1"
)

const maxRecent = 200

// DanmakuStore 负责弹幕在 Redis 中的读写
type DanmakuStore struct {
	rdb *redis.Client
}

// NewDanmakuStore 注入 Redis 客户端
func NewDanmakuStore(rdb *redis.Client) *DanmakuStore {
	return &DanmakuStore{rdb: rdb}
}

// key 生成某个房间的弹幕 List 键名
func key(roomID int64) string {
	return fmt.Sprintf("danmaku:room:%d", roomID)
}

// Append 写入一条弹幕，并把 List 裁剪到最近 200 条
func (s *DanmakuStore) Append(ctx context.Context, d *danmakuv1.Danmaku) error {
	payload, err := json.Marshal(d)
	if err != nil {
		return err
	}
	pipe := s.rdb.Pipeline()
	pipe.LPush(ctx, key(d.RoomId), payload)
	pipe.LTrim(ctx, key(d.RoomId), 0, maxRecent-1)
	_, err = pipe.Exec(ctx)
	return err
}

// Recent 返回按时间正序排列的最近弹幕
func (s *DanmakuStore) Recent(ctx context.Context, roomID int64) ([]*danmakuv1.Danmaku, error) {
	values, err := s.rdb.LRange(ctx, key(roomID), 0, maxRecent-1).Result()
	if err != nil {
		return nil, err
	}
	result := make([]*danmakuv1.Danmaku, 0, len(values))
	for i := len(values) - 1; i >= 0; i-- {
		d := &danmakuv1.Danmaku{}
		if err := json.Unmarshal([]byte(values[i]), d); err != nil {
			return nil, err
		}
		result = append(result, d)
	}
	return result, nil
}
