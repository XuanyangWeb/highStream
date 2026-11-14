package com.highstream.user.controller;

import com.highstream.proto.danmaku.v1.Danmaku;
import com.highstream.proto.danmaku.v1.DanmakuServiceGrpc;
import com.highstream.proto.danmaku.v1.SendDanmakuRequest;
import com.highstream.proto.danmaku.v1.SendDanmakuResponse;
import com.highstream.user.sse.DanmakuSseService;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// 弹幕 REST 接口：发送弹幕，以及把 gRPC 服务端流转成 SSE
@RestController
@RequestMapping("/danmaku")
public class DanmakuController {

    public record SendBody(long roomId, long userId, String content) {
    }

    public record DanmakuDto(long roomId, long userId, String content, long ts) {
        static DanmakuDto of(Danmaku danmaku) {
            return new DanmakuDto(danmaku.getRoomId(), danmaku.getUserId(),
                    danmaku.getContent(), danmaku.getTs());
        }
    }

    @GrpcClient("danmaku-service")
    private DanmakuServiceGrpc.DanmakuServiceBlockingStub danmakuService;

    private final DanmakuSseService sseService;

    public DanmakuController(DanmakuSseService sseService) {
        this.sseService = sseService;
    }

    // 发送弹幕
    @PostMapping
    public DanmakuDto send(@RequestBody SendBody body) {
        SendDanmakuResponse response = danmakuService.sendDanmaku(SendDanmakuRequest.newBuilder()
                .setRoomId(body.roomId())
                .setUserId(body.userId())
                .setContent(body.content() == null ? "" : body.content())
                .build());
        return DanmakuDto.of(response.getDanmaku());
    }

    // 订阅指定房间的弹幕，返回 text/event-stream
    @GetMapping(value = "/sse/{roomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable long roomId) {
        return sseService.subscribe(roomId);
    }
}
