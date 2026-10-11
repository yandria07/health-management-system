package com.health.healthserver.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET = "health-server-secret-key-must-be-32-chars-long";
    private static final long EXPIRE = 7 * 24 * 60 * 60 * 1000L; // 7 天

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // 生成 token
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))        // 👈 0.12.x 用 subject()，不是 setSubject()
                .claim("username", username)
                .issuedAt(new Date())                   // 👈 0.12.x 用 issuedAt()
                .expiration(new Date(System.currentTimeMillis() + EXPIRE)) // 👈 expiration()
                .signWith(getKey())                     // 👈 signWith(SecretKey) 即可，算法自动识别
                .compact();
    }

    // 解析 userId
    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()               // 👈 0.12.x 是 parser()，不是 parserBuilder()
                    .verifyWith(getKey())               // 👈 0.12.x 用 verifyWith()，不是 setSigningKey()
                    .build()
                    .parseSignedClaims(token)           // 👈 0.12.x 用 parseSignedClaims()，不是 parseClaimsJws()
                    .getPayload();                      // 👈 0.12.x 用 getPayload()，不是 getBody()
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}