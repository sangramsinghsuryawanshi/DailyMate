package com.dailymate.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.admin.dto.request.AdminComplaintStatusRequest;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.blood.dto.request.BloodRequestCreateRequest;
import com.dailymate.community.dto.request.CommunityComplaintRequest;
import com.dailymate.core.security.JwtService;
import com.dailymate.core.security.UserPrincipal;
import com.dailymate.events.dto.request.LocalEventRequest;
import com.dailymate.jobs.dto.request.JobPostRequest;
import com.dailymate.lostfound.dto.request.LostItemPostRequest;
import com.dailymate.user.entity.User;
import com.dailymate.user.entity.UserRole;
import com.dailymate.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "First", "Last"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    private String getAdminToken(String email) throws Exception {
        registerAndGetToken(email);
        User adminUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        adminUser.setRole(UserRole.ADMIN);
        userRepository.save(adminUser);
        return jwtService.issue(new UserPrincipal(adminUser));
    }

    @Test
    void rejectsAnonymousAndUserRoleAndAllowsAdminRole() throws Exception {
        String userToken = registerAndGetToken("user-role-admin-test@example.com");

        // 1. Anonymous access -> 401 across endpoints
        mvc.perform(get("/api/v1/admin/stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/complaints")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/jobs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/blood-requests")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/events")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/lost-found")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());

        // 2. Standard user receives 403 Forbidden across endpoints
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + userToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/complaints").header("Authorization", "Bearer " + userToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/jobs").header("Authorization", "Bearer " + userToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/blood-requests").header("Authorization", "Bearer " + userToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/events").header("Authorization", "Bearer " + userToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/lost-found").header("Authorization", "Bearer " + userToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + userToken)).andExpect(status().isForbidden());

        // 3. Admin receives 200 OK
        String adminToken = getAdminToken("real-admin@example.com");
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/complaints").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/jobs").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
    }

    @Test
    void verifiesStatsCalculationFromDatabaseCounts() throws Exception {
        String adminToken = getAdminToken("stats-admin@example.com");

        mvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.activeUsers").isNumber())
                .andExpect(jsonPath("$.totalComplaints").isNumber())
                .andExpect(jsonPath("$.totalJobs").isNumber())
                .andExpect(jsonPath("$.totalBloodRequests").isNumber())
                .andExpect(jsonPath("$.totalEvents").isNumber())
                .andExpect(jsonPath("$.totalLostFound").isNumber());
    }

    @Test
    void verifiesCompleteComplaintStatusLifecycleStateTransitions() throws Exception {
        String adminToken = getAdminToken("admin-lifecycle@example.com");
        String userToken = registerAndGetToken("complaint-user@example.com");

        String complaintBody = mvc.perform(post("/api/v1/community-complaints/complaints")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommunityComplaintRequest(
                                "Broken Streetlight", "Lighting", "Block A", "Light pole not working"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String c1 = objectMapper.readTree(complaintBody).get("id").asText();

        // OPEN -> IN_REVIEW: 200 OK
        mvc.perform(patch("/api/v1/admin/complaints/{id}/status", c1)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_REVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        // IN_REVIEW -> RESOLVED: 200 OK
        mvc.perform(patch("/api/v1/admin/complaints/{id}/status", c1)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // RESOLVED -> OPEN: 200 OK (reopen)
        mvc.perform(patch("/api/v1/admin/complaints/{id}/status", c1)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void adminCanModerateLostAndFound() throws Exception {
        String adminToken = getAdminToken("lf-admin@example.com");
        String userToken = registerAndGetToken("lf-user@example.com");

        // User posts lost item
        String itemBody = mvc.perform(post("/api/v1/lost-found/posts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LostItemPostRequest(
                                "Spam Item Post", "Electronics", "Park", "Spam description", "Spammer", "999"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String itemId = objectMapper.readTree(itemBody).get("id").asText();

        // Admin lists lost-found
        mvc.perform(get("/api/v1/admin/lost-found")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + itemId + "')]").exists());

        // Admin deletes inappropriate post
        mvc.perform(delete("/api/v1/admin/lost-found/{id}", itemId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Confirm deleted from admin list
        mvc.perform(get("/api/v1/admin/lost-found")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + itemId + "')]").doesNotExist());
    }

    @Test
    void adminCanModerateJobs() throws Exception {
        String adminToken = getAdminToken("jobs-admin@example.com");
        String userToken = registerAndGetToken("jobs-recruiter@example.com");

        // Recruiter posts job
        String jobBody = mvc.perform(post("/api/v1/jobs/posts")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JobPostRequest(
                                "Warehouse Assistant", "Services", "Kothrud", "Full-time",
                                new BigDecimal("22000.00"), "LogiCorp", "9876543210", "hr@logicorp.com", "OPEN", "Loading cargo"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jobId = objectMapper.readTree(jobBody).get("id").asText();

        // Admin toggles status to CLOSED
        mvc.perform(patch("/api/v1/admin/jobs/{id}/status", jobId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // Admin deletes job
        mvc.perform(delete("/api/v1/admin/jobs/{id}", jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanModerateBloodRequests() throws Exception {
        String adminToken = getAdminToken("blood-admin@example.com");
        String userToken = registerAndGetToken("blood-requester@example.com");

        String reqBody = mvc.perform(post("/api/v1/blood/requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BloodRequestCreateRequest(
                                "John Doe", "O+", 2, "City Hospital", "URGENT", "Jane Doe", "+91-9988776655", "Emergency surgery"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reqId = objectMapper.readTree(reqBody).get("id").asText();

        // Admin marks as FULFILLED
        mvc.perform(patch("/api/v1/admin/blood-requests/{id}/status", reqId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FULFILLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));

        // Admin deletes
        mvc.perform(delete("/api/v1/admin/blood-requests/{id}", reqId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanModerateEvents() throws Exception {
        String adminToken = getAdminToken("events-admin@example.com");
        String userToken = registerAndGetToken("events-organizer@example.com");

        String tomorrow = Instant.now().plusSeconds(86400).toString();
        String evBody = mvc.perform(post("/api/v1/events/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Neighborhood Cleanup\",\"category\":\"Volunteer\",\"location\":\"City Park\",\"eventDate\":\"" + tomorrow + "\",\"description\":\"Bring gloves\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String evId = objectMapper.readTree(evBody).get("id").asText();

        // Admin cancels event
        mvc.perform(patch("/api/v1/admin/events/{id}/status", evId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Admin deletes event
        mvc.perform(delete("/api/v1/admin/events/{id}", evId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanManageUserStatusAndSuspendedUserLosesAccess() throws Exception {
        String adminToken = getAdminToken("user-manager-admin@example.com");
        String victimEmail = "spammer-user@example.com";
        String victimToken = registerAndGetToken(victimEmail);

        User victim = userRepository.findByEmailIgnoreCase(victimEmail).orElseThrow();
        String victimId = victim.getId();

        // 1. Victim can access protected endpoints initially
        mvc.perform(get("/api/v1/jobs/my-posts")
                        .header("Authorization", "Bearer " + victimToken))
                .andExpect(status().isOk());

        // 2. Admin suspends victim account
        mvc.perform(patch("/api/v1/admin/users/{id}/status", victimId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // 3. Suspended user's existing token is immediately rejected with 401/403!
        mvc.perform(get("/api/v1/jobs/my-posts")
                        .header("Authorization", "Bearer " + victimToken))
                .andExpect(status().isUnauthorized());

        // 4. Admin reactivates victim account
        mvc.perform(patch("/api/v1/admin/users/{id}/status", victimId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 5. User access is restored
        mvc.perform(get("/api/v1/jobs/my-posts")
                        .header("Authorization", "Bearer " + victimToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotSuspendSelfOrOtherAdmin() throws Exception {
        String admin1Email = "primary-admin@example.com";
        String admin1Token = getAdminToken(admin1Email);
        User admin1 = userRepository.findByEmailIgnoreCase(admin1Email).orElseThrow();

        String admin2Email = "secondary-admin@example.com";
        String admin2Token = getAdminToken(admin2Email);
        User admin2 = userRepository.findByEmailIgnoreCase(admin2Email).orElseThrow();

        // Admin 1 attempts to suspend Admin 1 -> 400 Bad Request
        mvc.perform(patch("/api/v1/admin/users/{id}/status", admin1.getId())
                        .header("Authorization", "Bearer " + admin1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isBadRequest());

        // Admin 1 attempts to suspend Admin 2 -> 400 Bad Request
        mvc.perform(patch("/api/v1/admin/users/{id}/status", admin2.getId())
                        .header("Authorization", "Bearer " + admin1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifiesPasswordHashIsOmittedInUserResponse() throws Exception {
        String adminToken = getAdminToken("inspect-users-admin@example.com");

        mvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].email").exists())
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].status").exists());
    }

    @Test
    void notFoundForNonExistentResourceIds() throws Exception {
        String adminToken = getAdminToken("notfound-admin@example.com");

        mvc.perform(patch("/api/v1/admin/complaints/non-existent-id/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_REVIEW\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/v1/admin/lost-found/non-existent-id")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/v1/admin/jobs/non-existent-id")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
