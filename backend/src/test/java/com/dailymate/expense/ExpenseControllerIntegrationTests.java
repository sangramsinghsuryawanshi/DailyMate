package com.dailymate.expense;

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
class ExpenseControllerIntegrationTests {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsListsUpdatesAndDeletesExpenses() throws Exception {
        String registration =
                "{\"email\":\"expenses@example.com\",\"password\":\"correct-horse-battery-staple\",\"firstName\":\"Daily\",\"lastName\":\"Mate\"}";
        String body = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode session = objectMapper.readTree(body);
        String accessToken = session.get("accessToken").asText();

        String expenseBody = mvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Groceries\",\"description\":\"Weekly market run\",\"amount\":\"42.50\",\"spentOn\":\"2026-08-18\",\"notes\":\"Fresh vegetables\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Groceries"))
                .andExpect(jsonPath("$.amount").value(42.5))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode expense = objectMapper.readTree(expenseBody);
        String expenseId = expense.get("id").asText();

        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Weekly market run"));

        mvc.perform(patch("/api/v1/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Groceries\",\"description\":\"Market run and snacks\",\"amount\":\"58.00\",\"spentOn\":\"2026-08-19\",\"notes\":\"Added snacks\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Market run and snacks"))
                .andExpect(jsonPath("$.amount").value(58.0));

        mvc.perform(delete("/api/v1/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }
}
