package com.dailymate.lostfound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.lostfound.dto.request.LostItemPostRequest;
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
class LostFoundControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Lost", "Found"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void anonymousCanReadPublicPostsButCannotAccessPrivateEndpointsOrMutate() throws Exception {
        // Public feed -> 200 OK
        mvc.perform(get("/api/v1/lost-found/posts"))
                .andExpect(status().isOk());

        // My posts -> 401 Unauthorized
        mvc.perform(get("/api/v1/lost-found/my-posts"))
                .andExpect(status().isUnauthorized());

        LostItemPostRequest request = new LostItemPostRequest(
                "Lost Wallet", "Wallet", "City Park", "Brown leather wallet", "John", "555-0100");

        // Anonymous POST -> 401 Unauthorized
        mvc.perform(post("/api/v1/lost-found/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Anonymous PATCH -> 401 Unauthorized
        mvc.perform(patch("/api/v1/lost-found/posts/fake-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Anonymous DELETE -> 401 Unauthorized
        mvc.perform(delete("/api/v1/lost-found/posts/fake-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicFeedReturnsAllPostsWithCorrectUserIdAndChronologicalOrder() throws Exception {
        String tokenUserA = registerAndGetToken("user-a-lf@example.com");
        String tokenUserB = registerAndGetToken("user-b-lf@example.com");

        LostItemPostRequest reqA = new LostItemPostRequest(
                "Lost Blue Backpack", "Backpack", "Central Station", "Blue hiking backpack", "Ava", "555-1111");
        LostItemPostRequest reqB = new LostItemPostRequest(
                "Found Gold Watch", "Watch", "North Plaza", "Gold wristwatch near fountain", "Bob", "555-2222");

        String postBodyA = mvc.perform(post("/api/v1/lost-found/posts")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String postBodyB = mvc.perform(post("/api/v1/lost-found/posts")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode postA = objectMapper.readTree(postBodyA);
        JsonNode postB = objectMapper.readTree(postBodyB);

        assertTrue(postA.hasNonNull("userId"));
        assertTrue(postB.hasNonNull("userId"));

        // Public feed contains both
        String feedBody = mvc.perform(get("/api/v1/lost-found/posts"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode feed = objectMapper.readTree(feedBody);
        assertTrue(feed.isArray());
        assertTrue(feed.size() >= 2);
    }

    @Test
    void userCannotModifyOrDeleteAnotherUsersPost() throws Exception {
        String tokenUserA = registerAndGetToken("owner-lf@example.com");
        String tokenUserB = registerAndGetToken("other-lf@example.com");

        LostItemPostRequest reqA = new LostItemPostRequest(
                "Lost Laptop", "Electronics", "Library", "Silver laptop in black case", "Alice", "555-3333");

        String postBody = mvc.perform(post("/api/v1/lost-found/posts")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String postId = objectMapper.readTree(postBody).get("id").asText();

        // User B's my-posts does not have post A
        String myPostsB = mvc.perform(get("/api/v1/lost-found/my-posts")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode myPostsArray = objectMapper.readTree(myPostsB);
        for (JsonNode item : myPostsArray) {
            assertEquals(false, postId.equals(item.get("id").asText()));
        }

        // User B cannot PATCH post A -> 404
        LostItemPostRequest updateAttempt = new LostItemPostRequest(
                "Hijacked Post", "Electronics", "Library", "Hijacked", "Mallory", "555-9999");
        mvc.perform(patch("/api/v1/lost-found/posts/{id}", postId)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAttempt)))
                .andExpect(status().isNotFound());

        // User B cannot DELETE post A -> 404
        mvc.perform(delete("/api/v1/lost-found/posts/{id}", postId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanCreateUpdateAndCascadeDeletePost() throws Exception {
        String token = registerAndGetToken("crud-lf@example.com");

        LostItemPostRequest req = new LostItemPostRequest(
                "Lost Sunglasses", "Accessories", "Beach Pier", "Aviator sunglasses", "Sam", "555-4444");

        String postBody = mvc.perform(post("/api/v1/lost-found/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Lost Sunglasses"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String postId = objectMapper.readTree(postBody).get("id").asText();

        // Verify my-posts has the post
        mvc.perform(get("/api/v1/lost-found/my-posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(postId));

        // Update post
        LostItemPostRequest updateReq = new LostItemPostRequest(
                "Lost Sunglasses (Reward Offered)", "Accessories", "Beach Pier", "Aviator sunglasses - $20 reward", "Sam", "555-4444");

        mvc.perform(patch("/api/v1/lost-found/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lost Sunglasses (Reward Offered)"));

        // Delete post
        mvc.perform(delete("/api/v1/lost-found/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Now post is gone
        mvc.perform(get("/api/v1/lost-found/my-posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsInvalidPostPayloadWith400() throws Exception {
        String token = registerAndGetToken("invalid-lf@example.com");

        LostItemPostRequest invalid = new LostItemPostRequest("", "", "", "", "", "");

        mvc.perform(post("/api/v1/lost-found/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.itemType").exists())
                .andExpect(jsonPath("$.errors.location").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.contactName").exists())
                .andExpect(jsonPath("$.errors.contactPhone").exists());
    }
}
