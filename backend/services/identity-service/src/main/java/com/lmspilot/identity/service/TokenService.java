package com.lmspilot.identity.service;

import com.lmspilot.identity.domain.*;

import java.security.*;

import java.time.*;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import org.springframework.security.oauth2.jwt.*;

import org.springframework.stereotype.Service;
@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final AuthorizationService authorization;
    private final Duration accessTtl;
    public final Duration refreshTtl;
    private final SecureRandom random=new SecureRandom();
    public TokenService(JwtEncoder encoder,AuthorizationService authorization,@Value("${identity.access-token-ttl:PT15M}") Duration accessTtl,@Value("${identity.refresh-token-ttl:P7D}") Duration refreshTtl){
        this.encoder=encoder;
        this.authorization=authorization;
        this.accessTtl=accessTtl;
        this.refreshTtl=refreshTtl;
    }
    public record AccessToken(String value,long expiresInSeconds){
    }
    public AccessToken issueAccessToken(UserAccountEntity user,UUID sessionId){
        Instant now=Instant.now(),expiry=now.plus(accessTtl);
        Set<String> roles=new LinkedHashSet<>();
        user.roles.forEach(r->roles.add(r.code));
        String primary=roles.size()==1?roles.iterator().next():"STUDENT";
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer("lmspilot-identity").issuedAt(now).expiresAt(expiry).subject(user.id.toString()).claim("username",user.username).claim("fullName",user.fullName).claim("accountType",user.accountType.name()).claim("sid",sessionId.toString()).claim("mustChangePassword",user.mustChangePassword).claim("roles",roles).claim("primaryRole",primary).claim("permissions",authorization.permissionsForToken(user)).claim("globalPermissions",authorization.globalPermissionsForToken(user)).build();
        String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new AccessToken(token,accessTtl.toSeconds());
    }
    public String newRefreshToken(){
        byte[] b=new byte[48];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
    public String hashRefreshToken(String token){
        try{
            byte[] d=MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(d);
        }
        catch(Exception e){
            throw new IllegalStateException(e);
        }

    }

}
