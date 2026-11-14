package com.highstream.user.controller;

import com.highstream.proto.live.v1.CreateRoomRequest;
import com.highstream.proto.live.v1.CreateRoomResponse;
import com.highstream.proto.live.v1.ListRoomsRequest;
import com.highstream.proto.live.v1.ListRoomsResponse;
import com.highstream.proto.live.v1.LiveServiceGrpc;
import com.highstream.proto.live.v1.StartLiveRequest;
import com.highstream.proto.live.v1.StopLiveRequest;
import com.highstream.user.entity.RoomDto;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 直播间 REST 接口：把 HTTP 请求翻译成对 Go live-service 的 gRPC 调用
@RestController
@RequestMapping("/live")
public class LiveController {



    public record RoomListDto(List<RoomDto> rooms, int total) {
    }

    public record CreateRoomBody(long ownerId, String title) {
    }

    public record StartLiveBody(long ownerId) {
    }

    @GrpcClient("live-service")
    private LiveServiceGrpc.LiveServiceBlockingStub liveService;

    // 分页查询房间列表
    @GetMapping("/rooms")
    public RoomListDto list(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int pageSize) {
        ListRoomsResponse response = liveService.listRooms(ListRoomsRequest.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .build());
        List<RoomDto> rooms = response.getRoomsList().stream().map(RoomDto::of).toList();
        return new RoomListDto(rooms, response.getTotal());
    }

    // 创建房间
    @PostMapping("/rooms")
    public RoomDto create(@RequestBody CreateRoomBody body) {
        CreateRoomResponse response = liveService.createRoom(CreateRoomRequest.newBuilder()
                .setOwnerId(body.ownerId())
                .setTitle(body.title() == null ? "" : body.title())
                .build());
        return RoomDto.of(response.getRoom());
    }

    // 开播：Go 服务会回调 user-service 校验主播是否存在
    @PostMapping("/rooms/{id}/start")
    public RoomDto start(@PathVariable long id, @RequestBody StartLiveBody body) {

        return RoomDto.of(liveService.startLive(StartLiveRequest.newBuilder()
                .setRoomId(id)
                .setOwnerId(body.ownerId())
                .build()).getRoom());
    }

    // 下播
    @PostMapping("/rooms/{id}/stop")
    public RoomDto stop(@PathVariable long id) {
        return RoomDto.of(liveService.stopLive(StopLiveRequest.newBuilder()
                .setRoomId(id)
                .build()).getRoom());
    }
}
