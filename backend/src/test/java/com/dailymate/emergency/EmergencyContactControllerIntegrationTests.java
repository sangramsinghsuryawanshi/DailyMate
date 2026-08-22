package com.dailymate.emergency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.emergency.dto.request.EmergencyContactRequest;
import com.dailymate.emergency.entity.EmergencyContact;
import com.dailymate.emergency.repository.EmergencyContactRepository;
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
class EmergencyContactControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmergencyContactRepository emergencyContactRepository;

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Emergency", "User"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void anonymousCanReadPublicDirectoryButCannotAccessPrivateEndpointsOrMutate() throws Exception {
        // Public feed -> 200 OK
        mvc.perform(get("/api/v1/emergency-contacts/contacts"))
                .andExpect(status().isOk());

        // My contacts -> 401 Unauthorized
        mvc.perform(get("/api/v1/emergency-contacts/my-contacts"))
                .andExpect(status().isUnauthorized());

        EmergencyContactRequest req = new EmergencyContactRequest(
                "Family Doctor", "Personal", "+91 98765 43210", "Main Street", "Personal doctor");

        // Anonymous POST -> 401 Unauthorized
        mvc.perform(post("/api/v1/emergency-contacts/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // Anonymous PATCH -> 401 Unauthorized
        mvc.perform(patch("/api/v1/emergency-contacts/contacts/fake-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // Anonymous DELETE -> 401 Unauthorized
        mvc.perform(delete("/api/v1/emergency-contacts/contacts/fake-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicDirectoryReturnsOnlyUnownedVerifiedContactsAndSupportsCategoryFilter() throws Exception {
        // Seed a verified public contact (userId = null)
        EmergencyContact publicContact = new EmergencyContact();
        publicContact.setName("City Emergency Response");
        publicContact.setCategory("Ambulance");
        publicContact.setPhone("108");
        publicContact.setLocation("Citywide");
        publicContact.setDescription("24x7 Ambulance Dispatch");
        publicContact = emergencyContactRepository.save(publicContact);

        String publicFeed = mvc.perform(get("/api/v1/emergency-contacts/contacts?category=Ambulance"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode array = objectMapper.readTree(publicFeed);
        assertTrue(array.size() >= 1);
        for (JsonNode item : array) {
            assertEquals("Ambulance", item.get("category").asText());
            assertTrue(item.get("userId").isNull());
        }
    }

    @Test
    void regularUserCannotMutateOrDeleteVerifiedPublicContact() throws Exception {
        String token = registerAndGetToken("regular-user@example.com");

        // Seed a verified public contact (userId = null)
        EmergencyContact publicContact = new EmergencyContact();
        publicContact.setName("National Disaster Helpline");
        publicContact.setCategory("Helpline");
        publicContact.setPhone("1078");
        publicContact.setLocation("National");
        publicContact.setDescription("Disaster management helpline");
        publicContact = emergencyContactRepository.save(publicContact);

        String publicId = publicContact.getId();

        // User attempts to PATCH public contact -> 404 Not Found
        EmergencyContactRequest patchReq = new EmergencyContactRequest(
                "Hijacked Hotline", "Helpline", "000", "Nowhere", "Hijacked");
        mvc.perform(patch("/api/v1/emergency-contacts/contacts/{id}", publicId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isNotFound());

        // User attempts to DELETE public contact -> 404 Not Found
        mvc.perform(delete("/api/v1/emergency-contacts/contacts/{id}", publicId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        // Verify public contact remains unchanged
        EmergencyContact persisted = emergencyContactRepository.findById(publicId).orElseThrow();
        assertEquals("National Disaster Helpline", persisted.getName());
        assertEquals("1078", persisted.getPhone());
        assertNull(persisted.getUserId());
    }

    @Test
    void userCanCreateUpdateAndDeletePersonalEmergencyContact() throws Exception {
        String token = registerAndGetToken("personal-contact@example.com");

        EmergencyContactRequest createReq = new EmergencyContactRequest(
                "Dr. Alok Verma", "Hospital", "+91 91234 56789", "Apollo Hospital", "Family Cardiologist");

        String postBody = mvc.perform(post("/api/v1/emergency-contacts/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dr. Alok Verma"))
                .andExpect(jsonPath("$.phone").value("+91 91234 56789"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contactId = objectMapper.readTree(postBody).get("id").asText();

        // Check in /my-contacts
        mvc.perform(get("/api/v1/emergency-contacts/my-contacts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dr. Alok Verma"));

        // Update contact
        EmergencyContactRequest updateReq = new EmergencyContactRequest(
                "Dr. Alok Verma (Senior)", "Hospital", "+91 91234 99999", "Apollo Hospital Wing B", "Family Senior Cardiologist");

        mvc.perform(patch("/api/v1/emergency-contacts/contacts/{id}", contactId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Alok Verma (Senior)"))
                .andExpect(jsonPath("$.phone").value("+91 91234 99999"));

        // Delete contact -> 204 No Content
        mvc.perform(delete("/api/v1/emergency-contacts/contacts/{id}", contactId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void userCannotAccessOrMutateAnotherUsersPersonalContact() throws Exception {
        String tokenUserA = registerAndGetToken("contact-a@example.com");
        String tokenUserB = registerAndGetToken("contact-b@example.com");

        EmergencyContactRequest req = new EmergencyContactRequest(
                "Brother (Rahul)", "Personal", "+91 99999 11111", "Home", "Immediate family");

        String postBody = mvc.perform(post("/api/v1/emergency-contacts/contacts")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String contactId = objectMapper.readTree(postBody).get("id").asText();

        // User B's my-contacts does not contain User A's contact
        String myContactsB = mvc.perform(get("/api/v1/emergency-contacts/my-contacts")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode myContactsArray = objectMapper.readTree(myContactsB);
        for (JsonNode item : myContactsArray) {
            assertEquals(false, contactId.equals(item.get("id").asText()));
        }

        // User B cannot PATCH User A's contact -> 404
        EmergencyContactRequest hijacked = new EmergencyContactRequest(
                "Hijacked", "Personal", "000", "Nowhere", "Hijacked");
        mvc.perform(patch("/api/v1/emergency-contacts/contacts/{id}", contactId)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hijacked)))
                .andExpect(status().isNotFound());

        // User B cannot DELETE User A's contact -> 404
        mvc.perform(delete("/api/v1/emergency-contacts/contacts/{id}", contactId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidPayloadWith400() throws Exception {
        String token = registerAndGetToken("invalid-contact@example.com");

        // Blank name
        EmergencyContactRequest blankName = new EmergencyContactRequest(
                "", "Police", "100", "Central", "Police station");
        mvc.perform(post("/api/v1/emergency-contacts/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());

        // Blank phone
        EmergencyContactRequest blankPhone = new EmergencyContactRequest(
                "Police Station", "Police", "", "Central", "Police station");
        mvc.perform(post("/api/v1/emergency-contacts/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankPhone)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").exists());
    }
}
