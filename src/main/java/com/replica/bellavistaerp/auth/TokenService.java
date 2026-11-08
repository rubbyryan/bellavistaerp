package com.replica.bellavistaerp.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues and checks signed session tokens (HMAC-SHA256, JDK only).
 * Token = base64url(userId|ROLE|expiryEpochSeconds) + "." + base64url(signature).
 *
 * If app.auth.secret is empty a random key is created at start-up, so everyone
 * is signed out whenever the backend restarts. Set BV_AUTH_SECRET to keep
 * sessions across restarts.
 */
@Component
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final byte[] key;
    private final long ttlSeconds;

    public TokenService(@Value("${app.auth.secret:}") String secret,
                        @Value("${app.auth.token-hours:8}") long hours) {
        byte[] k;
        if (secret == null || secret.isBlank()) {
            k = new byte[32];
            new SecureRandom().nextBytes(k);
            log.warn("app.auth.secret is not set: using a random key, sessions end when the backend restarts.");
        } else {
            k = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = k;
        this.ttlSeconds = hours * 3600;
    }

    public String issue(AppUser user) {
        long expires = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = user.getId() + "|" + user.getRole().name() + "|" + expires;
        String body = B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + B64.encodeToString(sign(body));
    }

    /** Returns the user id when the token is genuine and not expired. */
    public Optional<Long> verify(String token) {
        try {
            int dot = token.indexOf('.');
            if (dot < 1) {
                return Optional.empty();
            }
            String body = token.substring(0, dot);
            byte[] given = B64D.decode(token.substring(dot + 1));
            if (!MessageDigest.isEqual(sign(body), given)) {
                return Optional.empty();
            }
            String[] parts = new String(B64D.decode(body), StandardCharsets.UTF_8).split("\\|");
            if (parts.length != 3 || Instant.now().getEpochSecond() > Long.parseLong(parts[2])) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(parts[0]));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private byte[] sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Token signing failed", e);
        }
    }
}
