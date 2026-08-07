package com.lmspilot.gateway;

import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

  @Bean
  public ReactiveJwtDecoder reactiveJwtDecoder(
      @Value("${lmspilot.jwt.secret}") String secret) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32)
      throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
    return NimbusReactiveJwtDecoder.withSecretKey(new SecretKeySpec(bytes, "HmacSHA256")).build();
  }

  @Bean
  public SecurityWebFilterChain gatewaySecurity(ServerHttpSecurity http, ReactiveJwtDecoder decoder) {
    Converter<Jwt, AbstractAuthenticationToken> converter = jwt -> {
      Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
      List<String> roles = jwt.getClaimAsStringList("roles");
      if (roles != null)
        roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
      List<String> perms = jwt.getClaimAsStringList("permissions");
      if (perms != null)
        perms.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
      String user = jwt.getClaimAsString("username");
      return new JwtAuthenticationToken(jwt, authorities, user == null ? jwt.getSubject() : user);
    };

    return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .exceptionHandling(e -> e.authenticationEntryPoint((exchange, ex) -> {
          ServerHttpResponse response = exchange.getResponse();
          response.setStatusCode(HttpStatus.UNAUTHORIZED);
          response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
          byte[] bytes = "{\"ok\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Phiên đăng nhập không hợp lệ hoặc đã hết hạn\"}".getBytes(StandardCharsets.UTF_8);
          return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        }))
        .authorizeExchange(a -> a.pathMatchers(
            "/actuator/health/**", "/actuator/info",
            "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
            "/public/v1/configuration", "/public/v1/branding", "/public/v1/branding/**",
            "/public/v1/certificates/**", "/public/v1/file-edit/**"
        ).permitAll().anyExchange().authenticated())
        .oauth2ResourceServer(o -> o
            .jwt(j -> j.jwtDecoder(decoder).jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(converter)))
            .authenticationEntryPoint((exchange, ex) -> {
              ServerHttpResponse response = exchange.getResponse();
              response.setStatusCode(HttpStatus.UNAUTHORIZED);
              response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
              byte[] bytes = "{\"ok\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Phiên đăng nhập không hợp lệ hoặc đã hết hạn\"}".getBytes(StandardCharsets.UTF_8);
              return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
            })
        )
        .build();
  }
}
