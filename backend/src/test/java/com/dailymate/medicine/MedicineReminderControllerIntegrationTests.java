package com.dailymate.medicine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.medicine.dto.request.MedicineReminderRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalTime;
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
class MedicineReminderControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        String registration = objectMapper.writeValueAsString(
                new RegisterRequest(email, "StrongPass123!", "Med", "User"));
        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void anonymousCannotAccessOrMutateReminders() throws Exception {
        mvc.perform(get("/api/v1/medicine-reminders"))
                .andExpect(status().isUnauthorized());

        MedicineReminderRequest request = new MedicineReminderRequest(
                "Aspirin", "100mg", "Daily", LocalTime.of(8, 0), "With water", true);

        mvc.perform(post("/api/v1/medicine-reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mvc.perform(patch("/api/v1/medicine-reminders/fake-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/v1/medicine-reminders/fake-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsAndListsRemindersInChronologicalOrder() throws Exception {
        String token = registerAndGetToken("chrono-order@example.com");

        // 1. Post Evening reminder (20:00)
        MedicineReminderRequest evening = new MedicineReminderRequest(
                "Melatonin", "5mg", "Nightly", LocalTime.of(20, 0), "Before sleep", true);
        mvc.perform(post("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(evening)))
                .andExpect(status().isCreated());

        // 2. Post Morning reminder (08:00)
        MedicineReminderRequest morning = new MedicineReminderRequest(
                "Thyroid Med", "50mcg", "Daily", LocalTime.of(8, 0), "Before breakfast", true);
        mvc.perform(post("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(morning)))
                .andExpect(status().isCreated());

        // 3. Post Afternoon reminder (12:30)
        MedicineReminderRequest midday = new MedicineReminderRequest(
                "Multivitamin", "1 tablet", "Daily", LocalTime.of(12, 30), "With lunch", true);
        mvc.perform(post("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(midday)))
                .andExpect(status().isCreated());

        // 4. Verify chronological order: 08:00 -> 12:30 -> 20:00
        mvc.perform(get("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Thyroid Med"))
                .andExpect(jsonPath("$[0].remindAt").value("08:00:00"))
                .andExpect(jsonPath("$[1].name").value("Multivitamin"))
                .andExpect(jsonPath("$[1].remindAt").value("12:30:00"))
                .andExpect(jsonPath("$[2].name").value("Melatonin"))
                .andExpect(jsonPath("$[2].remindAt").value("20:00:00"));
    }

    @Test
    void userCannotAccessOrModifyAnotherUsersReminders() throws Exception {
        String tokenUserA = registerAndGetToken("user-a-med@example.com");
        String tokenUserB = registerAndGetToken("user-b-med@example.com");

        // User A creates reminder
        MedicineReminderRequest request = new MedicineReminderRequest(
                "Blood Pressure Med", "10mg", "Daily", LocalTime.of(9, 0), "Morning", true);

        String createdBody = mvc.perform(post("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reminderId = objectMapper.readTree(createdBody).get("id").asText();

        // User B cannot see User A's reminder
        mvc.perform(get("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // User B cannot PATCH User A's reminder -> 404
        MedicineReminderRequest updateAttempt = new MedicineReminderRequest(
                "Hacked Med", "999mg", "Daily", LocalTime.of(9, 0), "Hacked", false);

        mvc.perform(patch("/api/v1/medicine-reminders/{id}", reminderId)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAttempt)))
                .andExpect(status().isNotFound());

        // User B cannot DELETE User A's reminder -> 404
        mvc.perform(delete("/api/v1/medicine-reminders/{id}", reminderId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidReminderPayloads() throws Exception {
        String token = registerAndGetToken("invalid-med@example.com");

        MedicineReminderRequest invalidRequest = new MedicineReminderRequest(
                "", "", "", null, "Notes", true);

        mvc.perform(post("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.dosage").exists())
                .andExpect(jsonPath("$.errors.frequency").exists())
                .andExpect(jsonPath("$.errors.remindAt").exists());
    }

    @Test
    void updatesAndDeletesOwnReminderSuccessfully() throws Exception {
        String token = registerAndGetToken("crud-med@example.com");

        MedicineReminderRequest initial = new MedicineReminderRequest(
                "Vitamin C", "500mg", "Daily", LocalTime.of(8, 0), "Morning", true);

        String createdBody = mvc.perform(post("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reminderId = objectMapper.readTree(createdBody).get("id").asText();

        // Update reminder
        MedicineReminderRequest updated = new MedicineReminderRequest(
                "Vitamin C", "1000mg", "Daily", LocalTime.of(8, 30), "With breakfast", false);

        mvc.perform(patch("/api/v1/medicine-reminders/{id}", reminderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dosage").value("1000mg"))
                .andExpect(jsonPath("$.remindAt").value("08:30:00"))
                .andExpect(jsonPath("$.active").value(false));

        // Delete reminder
        mvc.perform(delete("/api/v1/medicine-reminders/{id}", reminderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify list is empty
        mvc.perform(get("/api/v1/medicine-reminders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
