package com.dailymate.blood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.blood.dto.request.BloodRequestCreateRequest;
import com.dailymate.blood.dto.request.BloodRequestUpdateRequest;
import com.dailymate.blood.dto.request.DonationCenterRequest;
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
class BloodDonationControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Blood", "Donor"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void anonymousCanReadPublicRequestsAndCentersButCannotAccessPrivateEndpointsOrMutate() throws Exception {
        // Public feed -> 200 OK
        mvc.perform(get("/api/v1/blood/requests"))
                .andExpect(status().isOk());

        // Public centers -> 200 OK
        mvc.perform(get("/api/v1/blood/centers"))
                .andExpect(status().isOk());

        // My requests -> 401 Unauthorized
        mvc.perform(get("/api/v1/blood/my-requests"))
                .andExpect(status().isUnauthorized());

        BloodRequestCreateRequest request = new BloodRequestCreateRequest(
                "John Doe", "O+", 2, "City Hospital", "URGENT", "Jane Doe", "555-0100", "Needed immediately");

        // Anonymous POST -> 401 Unauthorized
        mvc.perform(post("/api/v1/blood/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Anonymous PATCH -> 401 Unauthorized
        mvc.perform(patch("/api/v1/blood/requests/fake-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Anonymous DELETE -> 401 Unauthorized
        mvc.perform(delete("/api/v1/blood/requests/fake-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicFeedSupportsBloodGroupAndStatusFiltering() throws Exception {
        String token = registerAndGetToken("filter-blood@example.com");

        BloodRequestCreateRequest reqO = new BloodRequestCreateRequest(
                "Alice O", "O+", 2, "General Hospital", "URGENT", "Dr. Bob", "555-1111", "Emergency surgery");
        BloodRequestCreateRequest reqA = new BloodRequestCreateRequest(
                "Charlie A", "A-", 1, "Apollo Clinic", "STANDARD", "Nurse Carol", "555-2222", "Scheduled operation");

        mvc.perform(post("/api/v1/blood/requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqO)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/blood/requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated());

        // Filter by Blood Group O+
        String oFeed = mvc.perform(get("/api/v1/blood/requests?bloodGroup=O+"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode oArray = objectMapper.readTree(oFeed);
        assertTrue(oArray.size() >= 1);
        for (JsonNode item : oArray) {
            assertEquals("O+", item.get("bloodGroup").asText());
        }

        // Filter by Status OPEN
        String openFeed = mvc.perform(get("/api/v1/blood/requests?status=OPEN"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode openArray = objectMapper.readTree(openFeed);
        assertTrue(openArray.size() >= 2);
    }

    @Test
    void userCannotModifyOrDeleteAnotherUsersBloodRequest() throws Exception {
        String tokenUserA = registerAndGetToken("owner-blood@example.com");
        String tokenUserB = registerAndGetToken("other-blood@example.com");

        BloodRequestCreateRequest createReq = new BloodRequestCreateRequest(
                "Patient A", "B+", 3, "Ruby Hall Clinic", "URGENT", "Relative A", "555-3333", "Urgent");

        String postBody = mvc.perform(post("/api/v1/blood/requests")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String requestId = objectMapper.readTree(postBody).get("id").asText();

        // User B's my-requests does not contain User A's request
        String myRequestsB = mvc.perform(get("/api/v1/blood/my-requests")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode myReqArray = objectMapper.readTree(myRequestsB);
        for (JsonNode item : myReqArray) {
            assertEquals(false, requestId.equals(item.get("id").asText()));
        }

        // User B cannot PATCH User A's request -> 404
        BloodRequestUpdateRequest hijackedUpdate = new BloodRequestUpdateRequest(
                "Hijacked", "B+", 1, "Nowhere", "STANDARD", "CANCELLED", "Mallory", "555-9999", "Hijack");
        mvc.perform(patch("/api/v1/blood/requests/{id}", requestId)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hijackedUpdate)))
                .andExpect(status().isNotFound());

        // User B cannot DELETE User A's request -> 404
        mvc.perform(delete("/api/v1/blood/requests/{id}", requestId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanCreateUpdateFulfillAndEnforceLifecycle() throws Exception {
        String token = registerAndGetToken("lifecycle-blood@example.com");

        BloodRequestCreateRequest createReq = new BloodRequestCreateRequest(
                "Patient Dave", "AB+", 2, "Sahyadri Hospital", "STANDARD", "Dave Sr", "555-4444", "Elective");

        String postBody = mvc.perform(post("/api/v1/blood/requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.bloodGroup").value("AB+"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String requestId = objectMapper.readTree(postBody).get("id").asText();

        // Update details while OPEN
        BloodRequestUpdateRequest updateReq = new BloodRequestUpdateRequest(
                "Patient Dave", "AB+", 3, "Sahyadri Hospital", "URGENT", "OPEN", "Dave Sr", "555-4444", "Urgency increased");

        mvc.perform(patch("/api/v1/blood/requests/{id}", requestId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsNeeded").value(3))
                .andExpect(jsonPath("$.urgency").value("URGENT"));

        // Transition OPEN -> FULFILLED (Allowed)
        BloodRequestUpdateRequest fulfillReq = new BloodRequestUpdateRequest(
                "Patient Dave", "AB+", 3, "Sahyadri Hospital", "URGENT", "FULFILLED", "Dave Sr", "555-4444", "Donation received");

        mvc.perform(patch("/api/v1/blood/requests/{id}", requestId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fulfillReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"));

        // Transition FULFILLED -> OPEN (Illegal transition -> 400 Bad Request)
        BloodRequestUpdateRequest illegalReopen = new BloodRequestUpdateRequest(
                "Patient Dave", "AB+", 3, "Sahyadri Hospital", "URGENT", "OPEN", "Dave Sr", "555-4444", "Reopen attempt");

        mvc.perform(patch("/api/v1/blood/requests/{id}", requestId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalReopen)))
                .andExpect(status().isBadRequest());

        // Delete request -> 204 No Content
        mvc.perform(delete("/api/v1/blood/requests/{id}", requestId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsInvalidBloodRequestPayloadWith400() throws Exception {
        String token = registerAndGetToken("invalid-blood@example.com");

        // Invalid blood group
        BloodRequestCreateRequest badGroup = new BloodRequestCreateRequest(
                "Patient", "INVALID_GROUP", 2, "Hospital", "STANDARD", "Contact", "555-1234", "Notes");

        mvc.perform(post("/api/v1/blood/requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badGroup)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.bloodGroup").exists());

        // 0 units needed (min is 1)
        BloodRequestCreateRequest zeroUnits = new BloodRequestCreateRequest(
                "Patient", "O+", 0, "Hospital", "STANDARD", "Contact", "555-1234", "Notes");

        mvc.perform(post("/api/v1/blood/requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroUnits)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.unitsNeeded").exists());

        // Blank patient name
        BloodRequestCreateRequest blankPatient = new BloodRequestCreateRequest(
                "", "O+", 1, "Hospital", "STANDARD", "Contact", "555-1234", "Notes");

        mvc.perform(post("/api/v1/blood/requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankPatient)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.patientName").exists());
    }

    @Test
    void donationCentersCrudRemainsFunctional() throws Exception {
        String token = registerAndGetToken("centers-blood@example.com");

        DonationCenterRequest req = new DonationCenterRequest(
                "City Blood Bank", "Downtown Plaza", "+1-555-0111", "24-hour donation support");

        String centerBody = mvc.perform(post("/api/v1/blood/centers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("City Blood Bank"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode center = objectMapper.readTree(centerBody);
        String centerId = center.get("id").asText();

        mvc.perform(get("/api/v1/blood/centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("City Blood Bank"));

        DonationCenterRequest updateReq = new DonationCenterRequest(
                "City Blood Bank West", "West End", "+1-555-0222", "Weekend donation support");

        mvc.perform(patch("/api/v1/blood/centers/{id}", centerId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("City Blood Bank West"));

        mvc.perform(delete("/api/v1/blood/centers/{id}", centerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
