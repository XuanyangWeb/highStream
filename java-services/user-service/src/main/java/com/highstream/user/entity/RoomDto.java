package com.highstream.user.entity;

import com.highstream.proto.live.v1.Room;
import com.highstream.user.controller.LiveController;

public record RoomDto(long id, String title, long ownerId, int status, String playUrl) {

    public static RoomDto of(Room room) {
        return new RoomDto(room.getId(), room.getTitle(), room.getOwnerId(),
                room.getStatus(), room.getPlayUrl());
    }
}
