package com.dailymate.auth.controller;

import com.dailymate.auth.dto.request.LoginRequest;
import com.dailymate.auth.dto.request.LogoutRequest;
import com.dailymate.auth.dto.request.RefreshTokenRequest;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.auth.dto.response.AuthResponse;
import com.dailymate.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a DailyMate account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in and receive an access token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access token")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return auth.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        auth.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
