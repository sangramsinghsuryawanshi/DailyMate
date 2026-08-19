package com.dailymate.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Test
    void registersAndLogsIn() throws Exception {
        String registration =
                "{\"email\":\"member@example.com\",\"password\":\"correct-horse-battery-staple\",\"firstName\":\"Daily\",\"lastName\":\"Mate\"}";
        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"password\":\"correct-horse-battery-staple\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        JsonNode response = objectMapper.readTree(body);
        mvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + response.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@example.com"));
    }

    @Test
    void rejectsDuplicateRegistration() throws Exception {
        String registration =
                "{\"email\":\"duplicate@example.com\",\"password\":\"correct-horse-battery-staple\",\"firstName\":\"Daily\",\"lastName\":\"Mate\"}";
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account already exists for this email address"));
    }

    @Test
    void rejectsInvalidLogin() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void refreshesTokensAndLogsOut() throws Exception {
        String registration =
                "{\"email\":\"refresh@example.com\",\"password\":\"correct-horse-battery-staple\",\"firstName\":\"Refresh\",\"lastName\":\"User\"}";
        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode session = objectMapper.readTree(body);
        String refreshToken = session.get("refreshToken").asText();

        String refreshedBody = mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode refreshed = objectMapper.readTree(refreshedBody);
        String newAccessToken = refreshed.get("accessToken").asText();
        String newRefreshToken = refreshed.get("refreshToken").asText();

        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("refresh@example.com"));

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefreshToken + "\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatesProfile() throws Exception {
        String registration =
                "{\"email\":\"profile@example.com\",\"password\":\"correct-horse-battery-staple\",\"firstName\":\"Daily\",\"lastName\":\"Mate\"}";
        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode session = objectMapper.readTree(body);
        String accessToken = session.get("accessToken").asText();

        mvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Updated\",\"lastName\":\"User\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    void rejectsExpiredAccessToken() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        String expiredToken = Jwts.builder()
                .subject("missing-user-id")
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(1800)))
                .signWith(key)
                .compact();

        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }
}
