package com.highstream.user.config;

import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// 把 gRPC 状态码统一翻译成 HTTP 状态码
@RestControllerAdvice
public class GrpcExceptionAdvice {

    // 处理阻塞 Stub 抛出的 StatusRuntimeException
    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<Map<String, String>> handle(StatusRuntimeException exception) {
        HttpStatus status = switch (exception.getStatus().getCode()) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case FAILED_PRECONDITION -> HttpStatus.PRECONDITION_FAILED;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        String description = exception.getStatus().getDescription();
        return ResponseEntity.status(status).body(Map.of(
                "code", exception.getStatus().getCode().name(),
                "message", description == null ? "" : description));
    }
}
