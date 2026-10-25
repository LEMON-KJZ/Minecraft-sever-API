package com.example.serverapi.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class BearerAuthenticator {
    public enum Failure { NONE, MISSING, MALFORMED, INVALID }
    public record AuthResult(boolean authenticated, Failure failure) {}

    private BearerAuthenticator() {}

    public static AuthResult authenticate(String authorization, String expectedKey) {
        if (authorization == null || authorization.isBlank()) return new AuthResult(false, Failure.MISSING);
        int separator = authorization.indexOf(' ');
        if (separator <= 0 || !authorization.substring(0, separator).equalsIgnoreCase("Bearer")) {
            return new AuthResult(false, Failure.MALFORMED);
        }
        String presented = authorization.substring(separator + 1).trim();
        if (presented.isEmpty() || expectedKey == null || expectedKey.isEmpty()) return new AuthResult(false, Failure.INVALID);
        boolean equal = MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), expectedKey.getBytes(StandardCharsets.UTF_8));
        return new AuthResult(equal, equal ? Failure.NONE : Failure.INVALID);
    }
}
