package com.dailymate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.LogoutRequest;
import com.dailymate.auth.dto.request.RefreshTokenRequest;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.user.entity.UserRole;
import com.dailymate.user.entity.UserStatus;
import com.dailymate.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Phase A: Authentication, JWT Lifecycle & Security Integration Audit.
 * Validates token rotation, replay protection, concurrent refresh races,
 * account status checks (LOCKED/DISABLED), CORS preflight, and Admin RBAC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationAndSecurityAuditIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode registerUser(String email, String password) throws Exception {
        String res = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, password, "Auth", "Audit"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(res);
    }

    @Test
    @DisplayName("Auth 1: Password Storage Uses BCrypt Hash (Never Plaintext)")
    void testPasswordBCryptStorage() throws Exception {
        String email = "bcrypt-audit@example.com";
        String rawPassword = "SecurePassword999!";
        registerUser(email, rawPassword);

        var userOpt = userRepository.findByEmailIgnoreCase(email);
        assertThat(userOpt).isPresent();
        String hash = userOpt.get().getPasswordHash();

        assertThat(hash).startsWith("$2a$") // BCrypt standard prefix
                .isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, hash)).isTrue();
    }

    @Test
    @DisplayName("Auth 2: Refresh Token Rotation & Replay Attack Invalidation")
    void testRefreshTokenRotationAndReplayRejection() throws Exception {
        JsonNode session = registerUser("replay-audit@example.com", "SecurePassword999!");
        String initialRefreshToken = session.get("refreshToken").asText();

        // 1. First rotation succeeds
        String refreshRes = mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String newRefreshToken = objectMapper.readTree(refreshRes).get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(initialRefreshToken);

        // 2. Replaying the INITIAL (already rotated) refresh token fails with 401
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("Invalid or expired refresh token")));

        // 3. New token works
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(newRefreshToken))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Auth 3: Concurrent Refresh Token Rotation Race -> Atomic Invalidation")
    void testConcurrentRefreshTokenRotationRace() throws Exception {
        JsonNode session = registerUser("concurrent-refresh@example.com", "SecurePassword999!");
        String initialRefreshToken = session.get("refreshToken").asText();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Integer>> tasks = List.of(
                    () -> mvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                            .andReturn().getResponse().getStatus(),
                    () -> mvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                            .andReturn().getResponse().getStatus(),
                    () -> mvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                            .andReturn().getResponse().getStatus(),
                    () -> mvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                            .andReturn().getResponse().getStatus()
            );

            List<Future<Integer>> results = executor.invokeAll(tasks);
            int successCount = 0;
            int unauthorizedCount = 0;
            for (Future<Integer> f : results) {
                int status = f.get();
                if (status == 200) successCount++;
                if (status == 401) unauthorizedCount++;
            }

            // Invariant: Exactly ONE request succeeds; the rest are rejected with 401
            assertThat(successCount).isEqualTo(1);
            assertThat(unauthorizedCount).isEqualTo(3);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Auth 4: Token Revocation on Logout")
    void testLogoutRevokesRefreshToken() throws Exception {
        JsonNode session = registerUser("logout-audit@example.com", "SecurePassword999!");
        String refreshToken = session.get("refreshToken").asText();

        // Logout
        mvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogoutRequest(refreshToken))))
                .andExpect(status().isNoContent());

        // Attempting to refresh with revoked token fails with 401
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("Invalid or expired refresh token")));
    }

    @Test
    @DisplayName("Auth 5: Locked / Disabled User Status Invalidates Access Token")
    void testLockedUserStatusBlocksAuthentication() throws Exception {
        String email = "locked-user@example.com";
        JsonNode session = registerUser(email, "SecurePassword999!");
        String accessToken = session.get("accessToken").asText();

        // Verify active user access
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Change status to SUSPENDED in database
        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        // Access token is rejected (401 Unauthorized) because user is disabled/suspended
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Auth 6: Admin Endpoint RBAC Enforcement (/api/v1/admin/**)")
    void testAdminEndpointRBAC() throws Exception {
        // Standard User
        JsonNode userSession = registerUser("regular-user@example.com", "SecurePassword999!");
        String userToken = userSession.get("accessToken").asText();

        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Admin User
        String adminEmail = "admin-rbac@example.com";
        registerUser(adminEmail, "SecurePassword999!");
        var adminUser = userRepository.findByEmailIgnoreCase(adminEmail).orElseThrow();
        adminUser.setRole(UserRole.ADMIN);
        userRepository.save(adminUser);

        // Re-authenticate to get token with ROLE_ADMIN
        String loginRes = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + adminEmail + "\",\"password\":\"SecurePassword999!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String adminToken = objectMapper.readTree(loginRes).get("accessToken").asText();

        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Auth 7: CORS Preflight Configuration Verification")
    void testCorsPreflightConfiguration() throws Exception {
        mvc.perform(options("/api/v1/expenses")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    @DisplayName("Auth 8: Public Infrastructure vs Private Actuators")
    void testPublicAndPrivateActuators() throws Exception {
        // /actuator/health is publicly accessible
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        // /actuator/env is not exposed (401 or 404)
        mvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }
}
