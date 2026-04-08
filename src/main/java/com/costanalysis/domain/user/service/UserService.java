package com.costanalysis.domain.user.service;

import com.costanalysis.domain.user.dto.*;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.domain.user.repository.UserRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@org.springframework.context.annotation.Profile("!mock")
public class UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserProfileRequest req) {
        User user = findUser(userId);
        user.setName(req.getName());
        if (req.getDepartment() != null) user.setDepartment(req.getDepartment());
        if (req.getPhone()      != null) user.setPhone(req.getPhone());
        if (req.getLanguage()   != null) user.setLanguage(req.getLanguage());
        if (req.getNotifyEmail() != null) user.setNotifyEmail(req.getNotifyEmail());
        if (req.getNotifyInApp() != null) user.setNotifyInApp(req.getNotifyInApp());
        return UserProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest req) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_PASSWORD);
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    // ── Admin operations ──────────────────────────────────────────────────────

    public List<UserProfileResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    @Transactional
    public UserProfileResponse createUser(UserCreateRequest req) {
        if (userRepository.existsByEmployeeId(req.getEmployeeId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_ID);
        }
        User user = User.builder()
                .employeeId(req.getEmployeeId())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .department(req.getDepartment())
                .phone(req.getPhone())
                .role(req.getRole() != null ? req.getRole() : "USER")
                .build();
        return UserProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public void toggleActive(Long userId, boolean active) {
        User user = findUser(userId);
        user.setActive(active);
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
