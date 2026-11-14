package com.highstream.gateway.token;

import com.highstream.gateway.error.ErrorEnum;
import com.highstream.gateway.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import io.netty.util.internal.StringUtil;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter, Order {


    private static final Logger LOG = LoggerFactory.getLogger(AuthGlobalFilter.class);

    private static final String AUTHORIZATION = "Authorization";

    private final JwtUtil jwtUtil;


    @Value("${spring.cloud.gateway.global-filter.auth.white-list}")
    private List<String> whiteList;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private AuthGlobalFilter(JwtUtil jwtUtil) {

        this.jwtUtil = jwtUtil;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }

    /**
     *
     * @param exchange the current server exchange
     * @param chain provides a way to delegate to the next filter
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        //获取请求参数
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        //白名单
        if (!CollectionUtils.isEmpty(whiteList)) {
            for (String whiteItem : whiteList) {
                if (pathMatcher.match(whiteItem, path)) {
                    return chain.filter(exchange);
                }
            }
        }
        // 获取Token
        String token = request.getHeaders().getFirst(AUTHORIZATION);
        ServerHttpResponse response = exchange.getResponse();
        if (StringUtil.isNullOrEmpty(token) || !token.startsWith("Bearer ")) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //去掉Bearer
        String realToken = token.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(realToken);
            if (jwtUtil.isTokenExpired(claims.getExpiration())) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            request = requestUtil(request, claims);
            return chain.filter(exchange.mutate().request(request).build());
        } catch (Exception e) {

            LOG.error(ErrorEnum.AUTHOR_ERROR.name(), e);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }
    }

    @Override
    public int value() {
        return 0;
    }

    private ServerHttpRequest requestUtil(ServerHttpRequest request, Claims claims) {
        return request.mutate()
                .header(GatewayHeaderEnum.X_TRACE_ID.getHeader(), claims.get("userId").toString())
                .header(GatewayHeaderEnum.X_USER_ROLE.getHeader(), claims.get("role").toString())
                .build();
    }
}
