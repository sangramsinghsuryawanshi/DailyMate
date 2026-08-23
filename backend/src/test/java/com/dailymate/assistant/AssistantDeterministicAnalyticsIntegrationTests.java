package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.AssistantAnalyticsDto.*;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.service.AssistantAnalyticsService;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.expense.dto.request.ExpenseEntryRequest;
import com.dailymate.expense.service.ExpenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
class AssistantDeterministicAnalyticsIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private AssistantAnalyticsService analyticsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest req = new RegisterRequest(email, "StrongPass123!", "Analytics", "Tester");
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    private String getUserIdFromToken(String token) throws Exception {
        String res = mvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(res).get("id").asText();
    }

    @Test
    void zeroPreviousPeriod_noInfinityOrNaN() throws Exception {
        String token = registerAndGetToken("zero-prev@example.com");
        String userId = getUserIdFromToken(token);

        // Add expense in Aug 2026, none in July 2026
        expenseService.createEntry(userId, new ExpenseEntryRequest("Food & Dining", "Dinner", new BigDecimal("500.00"), LocalDate.of(2026, 8, 10), null));

        PeriodComparisonDto comp = analyticsService.comparePeriods(userId, YearMonth.of(2026, 7), YearMonth.of(2026, 8));

        assertThat(comp.total1()).isEqualByComparingTo("0.00");
        assertThat(comp.total2()).isEqualByComparingTo("500.00");
        assertThat(comp.percentageChange()).isNull(); // Zero-denominator safe
        assertThat(comp.changeType()).isEqualTo(ChangeType.NEW_SPEND);

        mvc.perform(get("/api/v1/assistant/analytics/compare?period1=2026-07&period2=2026-08")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeType").value("NEW_SPEND"))
                .andExpect(jsonPath("$.percentageChange").doesNotExist());
    }

    @Test
    void zeroCurrentPeriod_calculatesCompleteReduction() throws Exception {
        String token = registerAndGetToken("zero-curr@example.com");
        String userId = getUserIdFromToken(token);

        // Add expense in July 2026, none in Aug 2026
        expenseService.createEntry(userId, new ExpenseEntryRequest("Utilities", "Electric", new BigDecimal("1200.00"), LocalDate.of(2026, 7, 5), null));

        PeriodComparisonDto comp = analyticsService.comparePeriods(userId, YearMonth.of(2026, 7), YearMonth.of(2026, 8));

        assertThat(comp.total1()).isEqualByComparingTo("1200.00");
        assertThat(comp.total2()).isEqualByComparingTo("0.00");
        assertThat(comp.percentageChange()).isEqualByComparingTo("-100.00");
        assertThat(comp.changeType()).isEqualTo(ChangeType.COMPLETE_REDUCTION);
    }

    @Test
    void positiveAndNegativeMoM_calculatesExactArithmetic() throws Exception {
        String token = registerAndGetToken("mom-math@example.com");
        String userId = getUserIdFromToken(token);

        // July: 1000, Aug: 1500 -> +50.00%
        expenseService.createEntry(userId, new ExpenseEntryRequest("Food & Dining", "Lunch", new BigDecimal("1000.00"), LocalDate.of(2026, 7, 1), null));
        expenseService.createEntry(userId, new ExpenseEntryRequest("Food & Dining", "Groceries", new BigDecimal("1500.00"), LocalDate.of(2026, 8, 1), null));

        PeriodComparisonDto comp = analyticsService.comparePeriods(userId, YearMonth.of(2026, 7), YearMonth.of(2026, 8));
        assertThat(comp.percentageChange()).isEqualByComparingTo("50.00");
        assertThat(comp.changeType()).isEqualTo(ChangeType.POSITIVE_GROWTH);

        // Aug: 1500, Sept: 750 -> -50.00%
        expenseService.createEntry(userId, new ExpenseEntryRequest("Food & Dining", "Snacks", new BigDecimal("750.00"), LocalDate.of(2026, 9, 1), null));
        PeriodComparisonDto comp2 = analyticsService.comparePeriods(userId, YearMonth.of(2026, 8), YearMonth.of(2026, 9));
        assertThat(comp2.percentageChange()).isEqualByComparingTo("-50.00");
        assertThat(comp2.changeType()).isEqualTo(ChangeType.NEGATIVE_GROWTH);
    }

    @Test
    void categoryRawTotalsEqualGrandTotal() throws Exception {
        String token = registerAndGetToken("category-sum@example.com");
        String userId = getUserIdFromToken(token);

        expenseService.createEntry(userId, new ExpenseEntryRequest("Food & Dining", "Lunch", new BigDecimal("350.50"), LocalDate.of(2026, 8, 10), null));
        expenseService.createEntry(userId, new ExpenseEntryRequest("Travel", "Cab", new BigDecimal("149.50"), LocalDate.of(2026, 8, 12), null));
        expenseService.createEntry(userId, new ExpenseEntryRequest("Utilities", "Internet", new BigDecimal("500.00"), LocalDate.of(2026, 8, 15), null));

        MonthlyAnalyticsReport report = analyticsService.generateMonthlyReport(userId, YearMonth.of(2026, 8));

        BigDecimal sumOfCategories = report.categories().stream()
                .map(CategoryBreakdownItem::rawTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Invariant: Raw category totals sum up exactly to the total expenditure
        assertThat(sumOfCategories).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(report.authoritativeTotal()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void monthBoundaryUsesHalfOpenIntervalAndConfiguredTimezone() throws Exception {
        String token = registerAndGetToken("timezone-boundary@example.com");
        String userId = getUserIdFromToken(token);

        // Entry on July 31 (outside Aug)
        expenseService.createEntry(userId, new ExpenseEntryRequest("Other", "Old", new BigDecimal("100.00"), LocalDate.of(2026, 7, 31), null));
        // Entry on Aug 1 (startInclusive)
        expenseService.createEntry(userId, new ExpenseEntryRequest("Other", "Start", new BigDecimal("200.00"), LocalDate.of(2026, 8, 1), null));
        // Entry on Aug 31 (inside Aug)
        expenseService.createEntry(userId, new ExpenseEntryRequest("Other", "End", new BigDecimal("300.00"), LocalDate.of(2026, 8, 31), null));
        // Entry on Sept 1 (nextPeriodStartExclusive)
        expenseService.createEntry(userId, new ExpenseEntryRequest("Other", "Next", new BigDecimal("400.00"), LocalDate.of(2026, 9, 1), null));

        PeriodExpenseSummary summary = analyticsService.getPeriodSummary(userId, YearMonth.of(2026, 8));

        // Must include exactly Aug 1 and Aug 31 (200 + 300 = 500)
        assertThat(summary.authoritativeTotal()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(summary.transactionCount()).isEqualTo(2);
    }

    @Test
    void emptyPeriod_deterministicResponse() throws Exception {
        String token = registerAndGetToken("empty-period@example.com");
        String userId = getUserIdFromToken(token);

        MonthlyAnalyticsReport report = analyticsService.generateMonthlyReport(userId, YearMonth.of(2025, 1));

        assertThat(report.authoritativeTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.categories()).isEmpty();
        assertThat(report.momComparison().changeType()).isEqualTo(ChangeType.NO_CHANGE);
    }

    @Test
    void aiAndBackendConsistency_exactInrMatching() throws Exception {
        String token = registerAndGetToken("ai-consistency@example.com");
        String userId = getUserIdFromToken(token);

        expenseService.createEntry(userId, new ExpenseEntryRequest("Food & Dining", "Thali", new BigDecimal("275.50"), LocalDate.of(2026, 8, 14), null));
        expenseService.createEntry(userId, new ExpenseEntryRequest("Utilities", "Mobile", new BigDecimal("499.00"), LocalDate.of(2026, 8, 15), null));

        // Assistant query: "How much did I spend in August?"
        mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("How much did I spend in August 2026?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("₹774.50")));
    }

    @Test
    void tenantIsolation_userBCannotAnalyzeUserA() throws Exception {
        String tokenA = registerAndGetToken("user-a-ana@example.com");
        String tokenB = registerAndGetToken("user-b-ana@example.com");
        String userAId = getUserIdFromToken(tokenA);

        expenseService.createEntry(userAId, new ExpenseEntryRequest("Health", "Medicines", new BigDecimal("999.00"), LocalDate.of(2026, 8, 1), null));

        // User B queries analytics for August 2026 -> Must see 0.00
        mvc.perform(get("/api/v1/assistant/analytics/monthly?period=2026-08")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authoritativeTotal").value(0.00));
    }
}
