package com.dailymate.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.notification.dto.request.NotificationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class NotificationControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Notification", "User"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void anonymousCannotAccessOrMutateNotifications() throws Exception {
        mvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());

        NotificationRequest request = new NotificationRequest(
                "Alert", "Time for medicine", "reminder", false, "MEDICINE", "med-1", "/medicines");

        mvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mvc.perform(patch("/api/v1/notifications/fake-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/v1/notifications/fake-id"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/notifications/mark-all-read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsAndListsNotificationsWithPaginationAndTargetUrls() throws Exception {
        String token = registerAndGetToken("notif-paged@example.com");

        NotificationRequest n1 = new NotificationRequest(
                "Medicine Reminder", "Take Vitamin D", "reminder", false, "MEDICINE", "med-1", "/medicines");
        NotificationRequest n2 = new NotificationRequest(
                "Expense Alert", "Monthly report ready", "info", false, "EXPENSE", "exp-1", "/expenses");

        mvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(n1)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(n2)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/notifications?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content[0].targetUrl").isNotEmpty());
    }

    @Test
    void userCannotAccessOrModifyAnotherUsersNotifications() throws Exception {
        String tokenUserA = registerAndGetToken("user-a-notif@example.com");
        String tokenUserB = registerAndGetToken("user-b-notif@example.com");

        NotificationRequest request = new NotificationRequest(
                "Private Alert", "User A confidential", "info", false, null, null, null);

        String createdBody = mvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String notificationId = objectMapper.readTree(createdBody).get("id").asText();

        // User B cannot see User A's notifications
        mvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        // User B cannot PATCH User A's notification -> 404
        NotificationRequest updateAttempt = new NotificationRequest(
                "Hacked", "Hacked Msg", "info", true, null, null, null);

        mvc.perform(patch("/api/v1/notifications/{id}", notificationId)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAttempt)))
                .andExpect(status().isNotFound());

        // User B cannot DELETE User A's notification -> 404
        mvc.perform(delete("/api/v1/notifications/{id}", notificationId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    void bulkMarkAllReadModifiesOnlyAuthenticatedUserNotifications() throws Exception {
        String tokenUserA = registerAndGetToken("bulk-a-notif@example.com");
        String tokenUserB = registerAndGetToken("bulk-b-notif@example.com");

        NotificationRequest unreadA = new NotificationRequest(
                "Alert A", "Msg A", "info", false, null, null, null);
        NotificationRequest unreadB = new NotificationRequest(
                "Alert B", "Msg B", "info", false, null, null, null);

        // User A creates unread notification
        mvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unreadA)))
                .andExpect(status().isCreated());

        // User B creates unread notification
        mvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unreadB)))
                .andExpect(status().isCreated());

        // User A calls mark-all-read
        mvc.perform(post("/api/v1/notifications/mark-all-read")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNoContent());

        // User A's notification is now read
        mvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].read").value(true));

        // User B's notification must remain unread (read = false)
        mvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].read").value(false));
    }

    @Test
    void rejectsInvalidNotificationPayloads() throws Exception {
        String token = registerAndGetToken("invalid-notif@example.com");

        NotificationRequest invalid = new NotificationRequest("", "", "", false, null, null, null);

        mvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.message").exists())
                .andExpect(jsonPath("$.errors.type").exists());
    }
}
