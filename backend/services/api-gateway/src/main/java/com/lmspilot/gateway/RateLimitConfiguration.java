package com.lmspilot.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;

import org.springframework.context.annotation.*;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import reactor.core.publisher.Mono;
@Configuration
public class RateLimitConfiguration{
    @Bean
    public KeyResolver userOrIpKeyResolver(){
        return exchange->ReactiveSecurityContextHolder.getContext().map(c->c.getAuthentication()==null?null:c.getAuthentication().getName()).filter(v->v!=null&&!v.isBlank()).switchIfEmpty(Mono.just(exchange.getRequest().getRemoteAddress()==null?"unknown":exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()));
    }

}
