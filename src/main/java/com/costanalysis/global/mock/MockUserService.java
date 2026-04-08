package com.costanalysis.global.mock;

import com.costanalysis.domain.user.dto.*;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.domain.user.service.UserService;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mock 프로파일 전용 사용자 서비스.
 */
@Slf4j
@Service
@Profile("mock")
public class MockUserService extends UserService {

    private final MockDataStore dataStore;

    public MockUserService(MockDataStore dataStore) {
        super(null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public UserProfileResponse getProfile(Long userId) {
        User user = findUser(userId);
        return UserProfileResponse.from(user);
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UserProfileRequest req) {
        User user = findUser(userId);
        user.setName(req.getName());
        if (req.getDepartment() != null) user.setDepartment(req.getDepartment());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getLanguage() != null) user.setLanguage(req.getLanguage());
        if (req.getNotifyEmail() != null) user.setNotifyEmail(req.getNotifyEmail());
        if (req.getNotifyInApp() != null) user.setNotifyInApp(req.getNotifyInApp());
        log.debug("[MOCK] 프로필 수정: {}", user.getName());
        return UserProfileResponse.from(user);
    }

    @Override
    public void changePassword(Long userId, PasswordChangeRequest req) {
        log.debug("[MOCK] 비밀번호 변경 (무시): userId={}", userId);
    }

    @Override
    public List<UserProfileResponse> listUsers() {
        return dataStore.getUsers().values().stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    @Override
    public UserProfileResponse createUser(UserCreateRequest req) {
        if (dataStore.findUserByEmployeeId(req.getEmployeeId()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_ID);
        }
        Long id = dataStore.getUserSeq().incrementAndGet();
        User user = User.builder()
                .id(id)
                .employeeId(req.getEmployeeId())
                .password("$2a$10$mock")
                .name(req.getName())
                .department(req.getDepartment())
                .phone(req.getPhone())
                .role(req.getRole() != null ? req.getRole() : "USER")
                .createdAt(OffsetDateTime.now())
                .build();
        dataStore.getUsers().put(id, user);
        log.debug("[MOCK] 사용자 생성: {}", user.getName());
        return UserProfileResponse.from(user);
    }

    @Override
    public void toggleActive(Long userId, boolean active) {
        User user = findUser(userId);
        user.setActive(active);
        log.debug("[MOCK] 계정 상태 변경: {} -> {}", user.getName(), active);
    }

    private User findUser(Long userId) {
        User user = dataStore.getUsers().get(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
