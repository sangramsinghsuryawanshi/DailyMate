package com.dailymate.user.controller;

import com.dailymate.auth.dto.response.UserResponse;
import com.dailymate.core.security.UserPrincipal;
import com.dailymate.user.dto.request.UpdateProfileRequest;
import com.dailymate.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return users.getProfile(principal.user().getId());
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return users.updateProfile(principal.user().getId(), request);
    }
}
