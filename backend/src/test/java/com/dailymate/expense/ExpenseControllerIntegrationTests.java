package com.dailymate.expense;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.expense.dto.request.ExpenseEntryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        String registration = objectMapper.writeValueAsString(
                new RegisterRequest(email, "StrongPass123!", "Expense", "User"));
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
    void anonymousCannotAccessOrMutateExpenses() throws Exception {
        mvc.perform(get("/api/v1/expenses"))
                .andExpect(status().isUnauthorized());

        ExpenseEntryRequest request = new ExpenseEntryRequest(
                "Groceries", "Snacks", new BigDecimal("150.00"), LocalDate.of(2026, 8, 20), null);

        mvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mvc.perform(patch("/api/v1/expenses/fake-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/v1/expenses/fake-id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsAndListsExpensesInDescendingDateOrderWithExactPrecision() throws Exception {
        String token = registerAndGetToken("chrono-expenses@example.com");

        // 1. Post expense on 2026-08-10 (Amount: 1234.56)
        ExpenseEntryRequest exp1 = new ExpenseEntryRequest(
                "Utilities", "Electricity Bill", new BigDecimal("1234.56"), LocalDate.of(2026, 8, 10), "Monthly bill");
        mvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exp1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1234.56));

        // 2. Post expense on 2026-08-20 (Amount: 500.00)
        ExpenseEntryRequest exp2 = new ExpenseEntryRequest(
                "Groceries", "Weekly Market Run", new BigDecimal("500.00"), LocalDate.of(2026, 8, 20), "Vegetables");
        mvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exp2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(500.00));

        // 3. Post expense on 2026-08-15 (Amount: 75.25)
        ExpenseEntryRequest exp3 = new ExpenseEntryRequest(
                "Dining", "Lunch with colleagues", new BigDecimal("75.25"), LocalDate.of(2026, 8, 15), "Cafe");
        mvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exp3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(75.25));

        // 4. Verify descending date order: 2026-08-20 -> 2026-08-15 -> 2026-08-10 and exact amounts
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].description").value("Weekly Market Run"))
                .andExpect(jsonPath("$[0].spentOn").value("2026-08-20"))
                .andExpect(jsonPath("$[0].amount").value(500.00))
                .andExpect(jsonPath("$[1].description").value("Lunch with colleagues"))
                .andExpect(jsonPath("$[1].spentOn").value("2026-08-15"))
                .andExpect(jsonPath("$[1].amount").value(75.25))
                .andExpect(jsonPath("$[2].description").value("Electricity Bill"))
                .andExpect(jsonPath("$[2].spentOn").value("2026-08-10"))
                .andExpect(jsonPath("$[2].amount").value(1234.56));
    }

    @Test
    void userCannotAccessOrModifyAnotherUsersExpenses() throws Exception {
        String tokenUserA = registerAndGetToken("user-a-exp@example.com");
        String tokenUserB = registerAndGetToken("user-b-exp@example.com");

        // User A creates expense
        ExpenseEntryRequest request = new ExpenseEntryRequest(
                "Health", "Doctor Consultation", new BigDecimal("800.00"), LocalDate.of(2026, 8, 18), "Checkup");

        String createdBody = mvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String expenseId = objectMapper.readTree(createdBody).get("id").asText();

        // User B cannot see User A's expense
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // User B cannot PATCH User A's expense -> 404
        ExpenseEntryRequest updateAttempt = new ExpenseEntryRequest(
                "Hacked", "Hacked Description", new BigDecimal("1.00"), LocalDate.of(2026, 8, 18), null);

        mvc.perform(patch("/api/v1/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAttempt)))
                .andExpect(status().isNotFound());

        // User B cannot DELETE User A's expense -> 404
        mvc.perform(delete("/api/v1/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidExpensePayloads() throws Exception {
        String token = registerAndGetToken("invalid-exp@example.com");

        // Blank category, description, missing spentOn, null amount
        ExpenseEntryRequest invalidRequest1 = new ExpenseEntryRequest(
                "", "", null, null, null);

        mvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.category").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.spentOn").exists());

        // Non-positive amount (0.00)
        ExpenseEntryRequest zeroAmountRequest = new ExpenseEntryRequest(
                "Groceries", "Zero amount item", new BigDecimal("0.00"), LocalDate.of(2026, 8, 20), null);

        mvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroAmountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());

        // Negative amount (-10.50)
        ExpenseEntryRequest negativeAmountRequest = new ExpenseEntryRequest(
                "Groceries", "Negative amount item", new BigDecimal("-10.50"), LocalDate.of(2026, 8, 20), null);

        mvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeAmountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void updatesAndDeletesOwnExpenseSuccessfully() throws Exception {
        String token = registerAndGetToken("crud-exp@example.com");

        ExpenseEntryRequest initial = new ExpenseEntryRequest(
                "Shopping", "New Shoes", new BigDecimal("2499.00"), LocalDate.of(2026, 8, 12), "Running shoes");

        String createdBody = mvc.perform(post("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String expenseId = objectMapper.readTree(createdBody).get("id").asText();

        // Update expense
        ExpenseEntryRequest updated = new ExpenseEntryRequest(
                "Shopping", "Running Shoes and Socks", new BigDecimal("2799.50"), LocalDate.of(2026, 8, 12), "Added socks");

        mvc.perform(patch("/api/v1/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Running Shoes and Socks"))
                .andExpect(jsonPath("$.amount").value(2799.50))
                .andExpect(jsonPath("$.notes").value("Added socks"));

        // Delete expense
        mvc.perform(delete("/api/v1/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify list is empty
        mvc.perform(get("/api/v1/expenses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
