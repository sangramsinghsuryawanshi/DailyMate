package com.dailymate.jobs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
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
class JobPostControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Job", "Poster"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void anonymousUsersHaveAccessToPublicPostsOnly() throws Exception {
        // Public posts feed allows anonymous
        mvc.perform(get("/api/v1/jobs/posts"))
                .andExpect(status().isOk());

        // My posts requires authentication
        mvc.perform(get("/api/v1/jobs/my-posts"))
                .andExpect(status().isUnauthorized());

        // Create job requires authentication
        mvc.perform(post("/api/v1/jobs/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tech Lead\",\"category\":\"Technical\",\"location\":\"Pune\",\"type\":\"Full-time\",\"description\":\"Lead a team\"}"))
                .andExpect(status().isUnauthorized());

        // Update job requires authentication
        mvc.perform(patch("/api/v1/jobs/posts/any-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tech Lead\",\"category\":\"Technical\",\"location\":\"Pune\",\"type\":\"Full-time\",\"description\":\"Lead a team\"}"))
                .andExpect(status().isUnauthorized());

        // Delete job requires authentication
        mvc.perform(delete("/api/v1/jobs/posts/any-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicFeedSupportsSearchAndFiltering() throws Exception {
        String token = registerAndGetToken("filter_tester@example.com");

        mvc.perform(post("/api/v1/jobs/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Senior Java Developer\",\"category\":\"Technical\",\"location\":\"Kothrud\",\"type\":\"Full-time\",\"salary\":120000.00,\"companyName\":\"DailyTech\",\"description\":\"Java Spring Boot engineer\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/jobs/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Store Cashier\",\"category\":\"Retail\",\"location\":\"Aundh\",\"type\":\"Part-time\",\"salary\":18000.00,\"companyName\":\"SuperMart\",\"description\":\"Cashier for evening shifts\"}"))
                .andExpect(status().isCreated());

        // Search by title
        mvc.perform(get("/api/v1/jobs/posts").param("search", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Senior Java Developer"));

        // Filter by category
        mvc.perform(get("/api/v1/jobs/posts").param("category", "Retail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Store Cashier"));

        // Filter by type
        mvc.perform(get("/api/v1/jobs/posts").param("type", "Part-time"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Store Cashier"));
    }

    @Test
    void userCanCreateManageAndCloseJobPosts() throws Exception {
        String token = registerAndGetToken("recruiter_lifecycle@example.com");

        // Create job (server defaults status to OPEN)
        String postBody = mvc.perform(post("/api/v1/jobs/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Community Manager\",\"category\":\"Services\",\"location\":\"Baner\",\"type\":\"Full-time\",\"salary\":45000.00,\"companyName\":\"CityGroup\",\"contactPhone\":\"+91-9876543210\",\"contactEmail\":\"jobs@citygroup.in\",\"description\":\"Manage neighborhood community initiatives\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Community Manager"))
                .andExpect(jsonPath("$.salary").value(45000.00))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode post = objectMapper.readTree(postBody);
        String postId = post.get("id").asText();

        // Fetch my-posts
        mvc.perform(get("/api/v1/jobs/my-posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(postId))
                .andExpect(jsonPath("$[0].companyName").value("CityGroup"));

        // Update job details and close status
        mvc.perform(patch("/api/v1/jobs/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Senior Community Manager\",\"category\":\"Services\",\"location\":\"Baner\",\"type\":\"Full-time\",\"salary\":55000.00,\"companyName\":\"CityGroup\",\"contactPhone\":\"+91-9876543210\",\"contactEmail\":\"jobs@citygroup.in\",\"status\":\"CLOSED\",\"description\":\"Updated role description\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior Community Manager"))
                .andExpect(jsonPath("$.salary").value(55000.00))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // Closed job is omitted from default public feed (which defaults to OPEN)
        mvc.perform(get("/api/v1/jobs/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + postId + "')]").doesNotExist());

        // But accessible with status=ALL
        mvc.perform(get("/api/v1/jobs/posts").param("status", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + postId + "')]").exists());

        // Delete job
        mvc.perform(delete("/api/v1/jobs/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Confirm deleted
        mvc.perform(get("/api/v1/jobs/my-posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + postId + "')]").doesNotExist());
    }

    @Test
    void crossTenantMutationsReturnNotFound() throws Exception {
        String tokenUserA = registerAndGetToken("user_a_jobs@example.com");
        String tokenUserB = registerAndGetToken("user_b_jobs@example.com");

        // User A creates a job post
        String postBody = mvc.perform(post("/api/v1/jobs/posts")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Private Gig\",\"category\":\"Other\",\"location\":\"Shivajinagar\",\"type\":\"Contract\",\"salary\":15000.00,\"description\":\"User A private posting\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String postId = objectMapper.readTree(postBody).get("id").asText();

        // User B attempts to update User A's job -> 404 Not Found
        mvc.perform(patch("/api/v1/jobs/posts/{id}", postId)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hijacked Gig\",\"category\":\"Other\",\"location\":\"Shivajinagar\",\"type\":\"Contract\",\"salary\":99999.00,\"description\":\"Tampered\"}"))
                .andExpect(status().isNotFound());

        // User B attempts to delete User A's job -> 404 Not Found
        mvc.perform(delete("/api/v1/jobs/posts/{id}", postId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesRequiredFieldsAndSalary() throws Exception {
        String token = registerAndGetToken("validator_jobs@example.com");

        // Blank title -> 400
        mvc.perform(post("/api/v1/jobs/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"category\":\"Technical\",\"location\":\"Pune\",\"type\":\"Full-time\",\"description\":\"Valid description\"}"))
                .andExpect(status().isBadRequest());

        // Blank location -> 400
        mvc.perform(post("/api/v1/jobs/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid Title\",\"category\":\"Technical\",\"location\":\"\",\"type\":\"Full-time\",\"description\":\"Valid description\"}"))
                .andExpect(status().isBadRequest());

        // Negative salary -> 400
        mvc.perform(post("/api/v1/jobs/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid Title\",\"category\":\"Technical\",\"location\":\"Pune\",\"type\":\"Full-time\",\"salary\":-500.00,\"description\":\"Valid description\"}"))
                .andExpect(status().isBadRequest());
    }
}
