package com.highstream.user.sse;

import com.highstream.proto.danmaku.v1.DanmakuServiceGrpc;
import com.highstream.proto.danmaku.v1.SubscribeRequest;
import com.highstream.proto.danmaku.v1.SubscribeResponse;
import jakarta.annotation.PreDestroy;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 把 gRPC 服务端流桥接成 SSE：每个订阅者占用一个线程消费流
@Service
public class DanmakuSseService {

    public record DanmakuEvent(long roomId, long userId, String content, long ts) {
    }

    @GrpcClient("danmaku-service")
    private DanmakuServiceGrpc.DanmakuServiceBlockingStub danmakuService;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 创建 SSE 连接，异步开始消费 gRPC 流
    public SseEmitter subscribe(long roomId) {
        SseEmitter emitter = new SseEmitter(0L);
        executor.execute(() -> bridge(roomId, emitter));
        return emitter;
    }

    // 循环读取 gRPC 流并写入 SSE，直到流结束或客户端断开
    private void bridge(long roomId, SseEmitter emitter) {
        Iterator<SubscribeResponse> iterator = danmakuService.subscribe(
                SubscribeRequest.newBuilder().setRoomId(roomId).build());
        try {
            while (iterator.hasNext()) {
                var danmaku = iterator.next().getDanmaku();
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(danmaku.getTs()))
                        .name("danmaku")
                        .data(new DanmakuEvent(danmaku.getRoomId(), danmaku.getUserId(),
                                danmaku.getContent(), danmaku.getTs())));
            }
            emitter.complete();
        } catch (Exception e) {
            // 客户端断开或 gRPC 流结束，关闭 SSE
            emitter.completeWithError(e);
        }
    }

    // 应用关闭时停止桥接线程
    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
