package com.dailymate.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.events.dto.request.LocalEventCreateRequest;
import com.dailymate.events.dto.request.LocalEventUpdateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class LocalEventControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Event", "Organizer"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void anonymousCanReadPublicEventsFeedButCannotAccessPrivateEndpointsOrMutate() throws Exception {
        // Public feed -> 200 OK
        mvc.perform(get("/api/v1/events/events"))
                .andExpect(status().isOk());

        // My events -> 401 Unauthorized
        mvc.perform(get("/api/v1/events/my-events"))
                .andExpect(status().isUnauthorized());

        LocalEventCreateRequest req = new LocalEventCreateRequest(
                "Community Cleanup", "Volunteer", "Riverside Park", Instant.now().plus(7, ChronoUnit.DAYS), "Neighborhood cleanup");

        // Anonymous POST -> 401 Unauthorized
        mvc.perform(post("/api/v1/events/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // Anonymous PATCH -> 401 Unauthorized
        mvc.perform(patch("/api/v1/events/events/fake-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        // Anonymous DELETE -> 401 Unauthorized
        mvc.perform(delete("/api/v1/events/events/fake-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicFeedSupportsCategoryStatusAndPastEventRepresentation() throws Exception {
        String token = registerAndGetToken("feed-events@example.com");

        Instant futureDate = Instant.now().plus(10, ChronoUnit.DAYS);
        Instant pastDate = Instant.now().minus(5, ChronoUnit.DAYS);

        LocalEventCreateRequest futureEvent = new LocalEventCreateRequest(
                "Football Tournament", "Sports", "Sports Complex", futureDate, "Annual tournament");
        LocalEventCreateRequest pastEvent = new LocalEventCreateRequest(
                "React Workshop", "Workshop", "Tech Hub", pastDate, "Historical workshop");

        mvc.perform(post("/api/v1/events/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureEvent)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/events/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pastEvent)))
                .andExpect(status().isCreated());

        // Filter by Category Sports
        String sportsFeed = mvc.perform(get("/api/v1/events/events?category=Sports"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode sportsArray = objectMapper.readTree(sportsFeed);
        assertTrue(sportsArray.size() >= 1);
        for (JsonNode item : sportsArray) {
            assertEquals("Sports", item.get("category").asText());
        }

        // Filter by Status PUBLISHED
        String publishedFeed = mvc.perform(get("/api/v1/events/events?status=PUBLISHED"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode pubArray = objectMapper.readTree(publishedFeed);
        assertTrue(pubArray.size() >= 2);
    }

    @Test
    void userCannotModifyOrDeleteAnotherUsersEvent() throws Exception {
        String tokenUserA = registerAndGetToken("owner-event@example.com");
        String tokenUserB = registerAndGetToken("other-event@example.com");

        LocalEventCreateRequest req = new LocalEventCreateRequest(
                "Art Exhibition", "Cultural", "Art Gallery", Instant.now().plus(14, ChronoUnit.DAYS), "Contemporary art");

        String postBody = mvc.perform(post("/api/v1/events/events")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = objectMapper.readTree(postBody).get("id").asText();

        // User B's my-events does not contain User A's event
        String myEventsB = mvc.perform(get("/api/v1/events/my-events")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode myEventsArray = objectMapper.readTree(myEventsB);
        for (JsonNode item : myEventsArray) {
            assertEquals(false, eventId.equals(item.get("id").asText()));
        }

        // User B cannot PATCH User A's event -> 404
        LocalEventUpdateRequest hijackedUpdate = new LocalEventUpdateRequest(
                "Hijacked Event", "Cultural", "Somewhere", Instant.now().plus(14, ChronoUnit.DAYS), "PUBLISHED", "Hijacked");
        mvc.perform(patch("/api/v1/events/events/{id}", eventId)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hijackedUpdate)))
                .andExpect(status().isNotFound());

        // User B cannot DELETE User A's event -> 404
        mvc.perform(delete("/api/v1/events/events/{id}", eventId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanCreateUpdateCancelReopenAndDeleteEvent() throws Exception {
        String token = registerAndGetToken("crud-events@example.com");

        LocalEventCreateRequest req = new LocalEventCreateRequest(
                "Music Jam Night", "Music", "Acoustic Cafe", Instant.now().plus(5, ChronoUnit.DAYS), "Open mic session");

        String postBody = mvc.perform(post("/api/v1/events/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String eventId = objectMapper.readTree(postBody).get("id").asText();

        // Update details while PUBLISHED
        LocalEventUpdateRequest updateReq = new LocalEventUpdateRequest(
                "Music Jam Night (Extended)", "Music", "Acoustic Cafe & Lounge", Instant.now().plus(5, ChronoUnit.DAYS), "PUBLISHED", "Extended open mic session");

        mvc.perform(patch("/api/v1/events/events/{id}", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Music Jam Night (Extended)"))
                .andExpect(jsonPath("$.location").value("Acoustic Cafe & Lounge"));

        // Transition PUBLISHED -> CANCELLED (Allowed)
        LocalEventUpdateRequest cancelReq = new LocalEventUpdateRequest(
                "Music Jam Night (Extended)", "Music", "Acoustic Cafe & Lounge", Instant.now().plus(5, ChronoUnit.DAYS), "CANCELLED", "Postponed due to rain");

        mvc.perform(patch("/api/v1/events/events/{id}", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Transition CANCELLED -> PUBLISHED (Reopened - Allowed)
        LocalEventUpdateRequest reopenReq = new LocalEventUpdateRequest(
                "Music Jam Night (Rescheduled)", "Music", "Acoustic Cafe & Lounge", Instant.now().plus(12, ChronoUnit.DAYS), "PUBLISHED", "Rescheduled date confirmed");

        mvc.perform(patch("/api/v1/events/events/{id}", eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reopenReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Delete event -> 204 No Content
        mvc.perform(delete("/api/v1/events/events/{id}", eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsInvalidEventPayloadWith400() throws Exception {
        String token = registerAndGetToken("invalid-events@example.com");

        // Blank title
        LocalEventCreateRequest blankTitle = new LocalEventCreateRequest(
                "", "Sports", "Stadium", Instant.now().plus(1, ChronoUnit.DAYS), "Description");
        mvc.perform(post("/api/v1/events/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankTitle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        // Blank location
        LocalEventCreateRequest blankLocation = new LocalEventCreateRequest(
                "Title", "Sports", "", Instant.now().plus(1, ChronoUnit.DAYS), "Description");
        mvc.perform(post("/api/v1/events/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankLocation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.location").exists());

        // Null date
        LocalEventCreateRequest nullDate = new LocalEventCreateRequest(
                "Title", "Sports", "Stadium", null, "Description");
        mvc.perform(post("/api/v1/events/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.eventDate").exists());
    }
}
