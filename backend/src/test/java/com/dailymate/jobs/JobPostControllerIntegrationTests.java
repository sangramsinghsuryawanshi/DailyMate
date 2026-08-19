package com.dailymate.jobs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    void createsListsUpdatesAndDeletesJobPosts() throws Exception {
        String postBody = mvc.perform(post("/api/v1/jobs/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Junior Cleaner\",\"category\":\"Services\",\"location\":\"South Hill\",\"type\":\"Part-time\",\"description\":\"Need a reliable cleaner for local apartments\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Junior Cleaner"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode post = objectMapper.readTree(postBody);
        String postId = post.get("id").asText();

        mvc.perform(get("/api/v1/jobs/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Junior Cleaner"));

        mvc.perform(patch("/api/v1/jobs/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Senior Cleaner\",\"category\":\"Services\",\"location\":\"North District\",\"type\":\"Full-time\",\"description\":\"Looking for an experienced cleaner to oversee multiple properties\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior Cleaner"));

        mvc.perform(delete("/api/v1/jobs/posts/{id}", postId))
                .andExpect(status().isNoContent());
    }
}
