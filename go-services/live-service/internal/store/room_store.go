package store

import (
	"context"
	"database/sql"
	"errors"
	"fmt"

	"highstream/live-service/pb/live/v1"
)

// 房间状态：0 未开播，1 直播中，2 已结束
const (
	StatusOffline int32 = 0
	StatusLiving  int32 = 1
	StatusEnded   int32 = 2
)

// 领域错误，由 handler 层转换成对应的 gRPC 状态码
var ErrRoomNotFound = errors.New("room not found")
var ErrNotOwner = errors.New("not room owner")
var ErrAlreadyLiving = errors.New("room is already living")

// RoomStore 负责 rooms 表的读写
type RoomStore struct {
	db *sql.DB
}

func NewRoomStore(db *sql.DB) *RoomStore {
	return &RoomStore{db: db}
}

// Create 新建房间，初始状态为未开播
func (s *RoomStore) Create(ctx context.Context, ownerID int64, title string) (*livev1.Room, error) {
	res, err := s.db.ExecContext(ctx,
		"INSERT INTO rooms(title, owner_id, status) VALUES(?, ?, ?)",
		title, ownerID, StatusOffline)
	if err != nil {
		return nil, fmt.Errorf("insert room: %w", err)
	}
	id, err := res.LastInsertId()
	if err != nil {
		return nil, err
	}
	return &livev1.Room{Id: id, Title: title, OwnerId: ownerID, Status: StatusOffline}, nil
}

// List 按 id 倒序分页查询房间，同时返回总数
func (s *RoomStore) List(ctx context.Context, page, pageSize int) ([]*livev1.Room, int32, error) {
	var total int32
	if err := s.db.QueryRowContext(ctx, "SELECT COUNT(*) FROM rooms").Scan(&total); err != nil {
		return nil, 0, err
	}
	rows, err := s.db.QueryContext(ctx,
		"SELECT id, title, owner_id, status, play_url FROM rooms ORDER BY id DESC LIMIT ? OFFSET ?",
		pageSize, (page-1)*pageSize)
	if err != nil {
		return nil, 0, err
	}
	defer rows.Close()

	var rooms []*livev1.Room
	for rows.Next() {
		room := &livev1.Room{}
		if err := rows.Scan(&room.Id, &room.Title, &room.OwnerId, &room.Status, &room.PlayUrl); err != nil {
			return nil, 0, err
		}
		rooms = append(rooms, room)
	}
	return rooms, total, rows.Err()
}

// Get 按 id 查询房间，不存在时返回 ErrRoomNotFound
func (s *RoomStore) Get(ctx context.Context, id int64) (*livev1.Room, error) {
	room := &livev1.Room{}
	err := s.db.QueryRowContext(ctx,
		"SELECT id, title, owner_id, status, play_url FROM rooms WHERE id = ?", id).
		Scan(&room.Id, &room.Title, &room.OwnerId, &room.Status, &room.PlayUrl)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrRoomNotFound
	}
	return room, err
}

// SetStatus 更新房间状态与播放地址，并返回更新后的房间
func (s *RoomStore) SetStatus(ctx context.Context, id int64, status int32, playURL string) (*livev1.Room, error) {
	if _, err := s.db.ExecContext(ctx,
		"UPDATE rooms SET status = ?, play_url = ? WHERE id = ?", status, playURL, id); err != nil {
		return nil, err
	}
	return s.Get(ctx, id)
}
