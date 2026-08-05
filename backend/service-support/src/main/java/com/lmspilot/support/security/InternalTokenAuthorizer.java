package com.lmspilot.support.security;

import com.lmspilot.support.api.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class InternalTokenAuthorizer {
    private final String expected;

    public InternalTokenAuthorizer(@Value("${lmspilot.internal-token}") String expected) { this.expected = expected; }

    public void require(String token) {
        byte[] supplied = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        byte[] configured = expected == null ? new byte[0] : expected.getBytes(StandardCharsets.UTF_8);
        if (configured.length == 0 || !MessageDigest.isEqual(supplied, configured)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SERVICE_TOKEN", "Internal service token is invalid");
        }
    }
}
