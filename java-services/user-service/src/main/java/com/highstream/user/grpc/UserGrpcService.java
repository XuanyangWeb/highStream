package com.highstream.user.grpc;

import com.highstream.proto.user.v1.GetUserRequest;
import com.highstream.proto.user.v1.GetUserResponse;
import com.highstream.proto.user.v1.UserServiceGrpc;
import com.highstream.user.repo.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

// 用户 gRPC 服务端：供 Go 服务回调查询用户
@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    public UserGrpcService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 查询用户，不存在时返回 NOT_FOUND
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        userRepository.findById(request.getId()).ifPresentOrElse(user -> {
            var protoUser = com.highstream.proto.user.v1.User.newBuilder()
                    .setId(user.getId())
                    .setName(user.getName())
                    .setAvatar(user.getAvatar())
                    .build();
            responseObserver.onNext(GetUserResponse.newBuilder().setUser(protoUser).build());
            responseObserver.onCompleted();
        }, () -> responseObserver.onError(Status.NOT_FOUND
                .withDescription("user " + request.getId() + " not found")
                .asRuntimeException()));
    }
}
