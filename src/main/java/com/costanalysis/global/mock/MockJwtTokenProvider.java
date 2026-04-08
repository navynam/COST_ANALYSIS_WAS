package com.costanalysis.global.mock;

import com.costanalysis.global.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Mock 프로파일 전용 JwtTokenProvider.
 * 실제 JWT 서명/검증 대신 간단한 토큰을 발급하고,
 * 토큰에서 userId/role을 추출한다.
 */
@Component
@Profile("mock")
public class MockJwtTokenProvider extends JwtTokenProvider {

    private static final String MOCK_SECRET = "mock-secret-key-for-development-only-must-be-at-least-32-bytes-long";
    private final SecretKey key = Keys.hmacShaKeyFor(MOCK_SECRET.getBytes(StandardCharsets.UTF_8));

    @Override
    public void init() {
        // mock에서는 @PostConstruct 초기화 불필요
    }

    @Override
    public String createAccessToken(Long userId, String employeeId, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("empId", employeeId)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400_000L))
                .signWith(key)
                .compact();
    }

    @Override
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 604800_000L))
                .signWith(key)
                .compact();
    }

    @Override
    public Claims parseToken(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            // mock 모드: 토큰 파싱 실패 시 기본 userId=1, role=ADMIN 반환
            return Jwts.claims()
                    .subject("1")
                    .add("role", "ADMIN")
                    .build();
        }
    }

    @Override
    public Long getUserId(String token) {
        try {
            return Long.valueOf(parseToken(token).getSubject());
        } catch (Exception e) {
            return 1L; // 기본 관리자
        }
    }

    @Override
    public String getRole(String token) {
        try {
            return parseToken(token).get("role", String.class);
        } catch (Exception e) {
            return "ADMIN";
        }
    }

    @Override
    public long getRefreshExpiry() {
        return 604800;
    }

    @Override
    public long getAccessExpiry() {
        return 86400;
    }
}
