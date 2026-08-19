package com.dailymate.lostfound;

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
class LostFoundControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsListsUpdatesAndDeletesLostItemPosts() throws Exception {
        String registration =
                "{\"email\":\"lostfound@example.com\",\"password\":\"correct-horse-battery-staple\",\"firstName\":\"Daily\",\"lastName\":\"Mate\"}";
        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode session = objectMapper.readTree(body);
        String accessToken = session.get("accessToken").asText();

        String postBody = mvc.perform(post("/api/v1/lost-found/posts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lost blue backpack\",\"itemType\":\"Backpack\",\"location\":\"Central station\",\"description\":\"Blue hiking backpack with red zipper.\",\"contactName\":\"Ava\",\"contactPhone\":\"+1-555-1111\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Lost blue backpack"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode post = objectMapper.readTree(postBody);
        String postId = post.get("id").asText();

        mvc.perform(get("/api/v1/lost-found/posts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Lost blue backpack"));

        mvc.perform(patch("/api/v1/lost-found/posts/{id}", postId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lost blue backpack updated\",\"itemType\":\"Backpack\",\"location\":\"North station\",\"description\":\"Updated description with tag details.\",\"contactName\":\"Ava\",\"contactPhone\":\"+1-555-2222\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lost blue backpack updated"));

        mvc.perform(delete("/api/v1/lost-found/posts/{id}", postId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }
}
