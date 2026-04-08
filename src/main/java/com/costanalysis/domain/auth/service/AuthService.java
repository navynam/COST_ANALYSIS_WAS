package com.costanalysis.domain.auth.service;

import com.costanalysis.domain.auth.dto.LoginRequest;
import com.costanalysis.domain.auth.dto.LoginResponse;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.domain.user.repository.UserRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import com.costanalysis.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!mock")
public class AuthService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtTokenProvider  jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken  = jwtTokenProvider.createAccessToken(user.getId(), user.getEmployeeId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // Refresh token 을 Redis에 저장
        redisTemplate.opsForValue().set(
                "refresh:" + user.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshExpiry(),
                TimeUnit.SECONDS
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresIn(jwtTokenProvider.getAccessExpiry())
                .userId(user.getId())
                .name(user.getName())
                .role(user.getRole())
                .department(user.getDepartment())
                .build();
    }

    public void logout(String accessToken) {
        var claims = jwtTokenProvider.parseToken(accessToken);
        long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remaining > 0) {
            redisTemplate.opsForValue().set(
                    "blacklist:token:" + accessToken,
                    "1",
                    remaining,
                    TimeUnit.MILLISECONDS
            );
        }
        Long userId = Long.valueOf(claims.getSubject());
        redisTemplate.delete("refresh:" + userId);
    }

    public LoginResponse refresh(String refreshToken) {
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);
        if (!refreshToken.equals(stored)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newAccess  = jwtTokenProvider.createAccessToken(user.getId(), user.getEmployeeId(), user.getRole());
        String newRefresh = jwtTokenProvider.createRefreshToken(user.getId());
        redisTemplate.opsForValue().set("refresh:" + userId, newRefresh,
                jwtTokenProvider.getRefreshExpiry(), TimeUnit.SECONDS);

        return LoginResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .accessExpiresIn(jwtTokenProvider.getAccessExpiry())
                .userId(user.getId())
                .name(user.getName())
                .role(user.getRole())
                .department(user.getDepartment())
                .build();
    }
}
