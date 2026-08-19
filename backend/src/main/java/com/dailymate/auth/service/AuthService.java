package com.dailymate.auth.service;

import com.dailymate.auth.dto.request.LoginRequest;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.auth.dto.response.AuthResponse;
import com.dailymate.auth.mapper.UserMapper;
import com.dailymate.core.exception.ConflictException;
import com.dailymate.core.security.JwtService;
import com.dailymate.core.security.UserPrincipal;
import com.dailymate.user.entity.User;
import com.dailymate.user.entity.UserRole;
import com.dailymate.user.entity.UserStatus;
import com.dailymate.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final UserMapper mapper;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwords,
            AuthenticationManager authenticationManager,
            JwtService jwt,
            RefreshTokenService refreshTokens,
            UserMapper mapper) {
        this.users = users;
        this.passwords = passwords;
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
        this.mapper = mapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account already exists for this email address");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return response(new UserPrincipal(users.save(user)));
    }

    public AuthResponse login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.email().trim().toLowerCase(Locale.ROOT), request.password()));
            return response((UserPrincipal) authentication.getPrincipal());
        } catch (BadCredentialsException exception) {
            throw new com.dailymate.core.exception.UnauthorizedException("Invalid email or password");
        }
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        User user = refreshTokens.rotate(rawRefreshToken);
        return response(new UserPrincipal(user));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revoke(rawRefreshToken);
    }

    private AuthResponse response(UserPrincipal principal) {
        var issuedRefreshToken = refreshTokens.issue(principal.user());
        return new AuthResponse(
                jwt.issue(principal),
                issuedRefreshToken.token(),
                "Bearer",
                jwt.expiresInSeconds(),
                issuedRefreshToken.expiresInSeconds(),
                mapper.toResponse(principal.user()));
    }
}
