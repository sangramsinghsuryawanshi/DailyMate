package com.dailymate.grocery;

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
class GroceryComparisonControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsListsUpdatesAndDeletesGroceryItems() throws Exception {
        String itemBody = mvc.perform(post("/api/v1/grocery/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rice\",\"category\":\"Grains\",\"store\":\"Fresh Mart\",\"price\":22.50,\"location\":\"Downtown\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rice"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode item = objectMapper.readTree(itemBody);
        String itemId = item.get("id").asText();

        mvc.perform(get("/api/v1/grocery/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rice"));

        mvc.perform(patch("/api/v1/grocery/items/{id}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rice\",\"category\":\"Grains\",\"store\":\"Better Basket\",\"price\":19.99,\"location\":\"West End\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store").value("Better Basket"));

        mvc.perform(delete("/api/v1/grocery/items/{id}", itemId))
                .andExpect(status().isNoContent());
    }
}
