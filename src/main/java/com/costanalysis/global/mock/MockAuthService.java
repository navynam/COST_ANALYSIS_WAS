package com.costanalysis.global.mock;

import com.costanalysis.domain.auth.dto.LoginRequest;
import com.costanalysis.domain.auth.dto.LoginResponse;
import com.costanalysis.domain.auth.service.AuthService;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import com.costanalysis.global.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Mock 프로파일 전용 인증 서비스.
 * admin1/1234 -> 김관리, analyst1/1234 -> 이분석, verifier1/1234 -> 박검증
 */
@Slf4j
@Service
@Profile("mock")
public class MockAuthService extends AuthService {

    private final MockDataStore dataStore;
    private final JwtTokenProvider jwtTokenProvider;

    public MockAuthService(MockDataStore dataStore, JwtTokenProvider jwtTokenProvider) {
        super(null, null, null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.debug("[MOCK] 로그인 시도: {}", request.getEmployeeId());

        User user = dataStore.findUserByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // Mock에서는 비밀번호 1234 고정
        if (!"1234".equals(request.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmployeeId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        log.info("[MOCK] 로그인 성공: {} ({})", user.getName(), user.getRole());

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

    @Override
    public void logout(String accessToken) {
        log.debug("[MOCK] 로그아웃 (무시)");
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        log.debug("[MOCK] 토큰 갱신");
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = dataStore.getUsers().get(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String newAccess = jwtTokenProvider.createAccessToken(user.getId(), user.getEmployeeId(), user.getRole());
        String newRefresh = jwtTokenProvider.createRefreshToken(user.getId());

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
