package com.dailymate.user.service;

import com.dailymate.auth.dto.response.UserResponse;
import com.dailymate.auth.mapper.UserMapper;
import com.dailymate.core.exception.UnauthorizedException;
import com.dailymate.user.dto.request.UpdateProfileRequest;
import com.dailymate.user.entity.User;
import com.dailymate.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final UserMapper mapper;

    public UserService(UserRepository users, UserMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    public UserResponse getProfile(String userId) {
        return mapper.toResponse(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        return mapper.toResponse(users.save(user));
    }

    private User findUser(String userId) {
        return users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }
}
