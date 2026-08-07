package com.lmspilot.gateway;

import java.util.UUID;

import org.slf4j.*;

import org.springframework.cloud.gateway.filter.*;

import org.springframework.core.*;

import org.springframework.core.annotation.Order;

import org.springframework.http.server.reactive.ServerHttpRequest;

import org.springframework.stereotype.Component;

import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationAndAccessLogFilter implements GlobalFilter{
    public static final String CORRELATION_HEADER="X-Correlation-Id";
    private static final Logger log=LoggerFactory.getLogger(CorrelationAndAccessLogFilter.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain){
        long started=System.nanoTime();
        String id=exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);
        if(id==null||id.isBlank())id=UUID.randomUUID().toString();
        ServerHttpRequest request=exchange.getRequest().mutate().header(CORRELATION_HEADER,id).build();
        exchange.getResponse().getHeaders().set(CORRELATION_HEADER,id);
        String finalId=id;
        return chain.filter(exchange.mutate().request(request).build()).doFinally(signal->{
            long ms=(System.nanoTime()-started)/1_000_000;
            Integer status=exchange.getResponse().getStatusCode()==null?null:exchange.getResponse().getStatusCode().value();
            log.info("access method={} path={} status={} durationMs={} correlationId={}",request.getMethod(),request.getURI().getPath(),status,ms,finalId);
        }
        );
    }

}
