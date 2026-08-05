package com.lmspilot.gateway;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-50)
public class MustChangePasswordFilter implements GlobalFilter {
    private static final Set<String> ALLOWED = Set.of(
        "/api/v1/auth/me",
        "/api/v1/auth/change-password",
        "/api/v1/auth/logout",
        "/api/v1/auth/sessions"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (ALLOWED.contains(path) || path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }
        return ReactiveSecurityContextHolder.getContext()
            .map(context -> context.getAuthentication())
            .filter(JwtAuthenticationToken.class::isInstance)
            .cast(JwtAuthenticationToken.class)
            .flatMap(authentication -> {
                Boolean mustChange = authentication.getToken().getClaimAsBoolean("mustChangePassword");
                if (!Boolean.TRUE.equals(mustChange)) {
                    return chain.filter(exchange);
                }
                var response = exchange.getResponse();
                response.setStatusCode(HttpStatus.PRECONDITION_REQUIRED);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                String json = "{\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"Bạn phải đổi mật khẩu tạm thời trước khi tiếp tục\"}";
                byte[] body = json.getBytes(StandardCharsets.UTF_8);
                return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
            })
            .switchIfEmpty(chain.filter(exchange));
    }
}
