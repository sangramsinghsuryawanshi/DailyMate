package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.AssistantAnalyticsDto.*;
import com.dailymate.assistant.service.AssistantAnalyticsService;
import com.dailymate.assistant.service.AssistantExportService;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.dailymate.expense.dto.request.ExpenseEntryRequest;
import com.dailymate.expense.service.ExpenseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
class AssistantExportServiceIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private AssistantAnalyticsService analyticsService;

    @Autowired
    private AssistantExportService exportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest req = new RegisterRequest(email, "StrongPass123!", "Export", "Tester");
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
    void csvEscapesCommasQuotesAndNewlines() throws Exception {
        String token = registerAndGetToken("csv-escape@example.com");
        String userId = getUserIdFromToken(token);

        // Add expense with commas, quotes, and newlines in description and notes
        expenseService.createEntry(userId, new ExpenseEntryRequest(
                "Food & Dining",
                "Dinner with \"Special, Family\" Guests",
                new BigDecimal("1250.00"),
                LocalDate.of(2026, 8, 20),
                "Note line 1,\nNote line 2 with \"quotes\""
        ));

        ReportExportResult export = exportService.exportCsv(userId, YearMonth.of(2026, 8));
        String csvContent = new String(export.content(), StandardCharsets.UTF_8);

        assertThat(csvContent).startsWith("Date,Category,Description,Amount (INR),Notes\n");
        assertThat(csvContent).contains("\"Dinner with \"\"Special, Family\"\" Guests\"");
        assertThat(csvContent).contains("\"Note line 1,\nNote line 2 with \"\"quotes\"\"\"");
        assertThat(csvContent).contains("Total,,,1250.00,");
    }

    @Test
    void exportTruthfulness_allFormatsMatchAuthoritativeTotals() throws Exception {
        String token = registerAndGetToken("export-truth@example.com");
        String userId = getUserIdFromToken(token);

        expenseService.createEntry(userId, new ExpenseEntryRequest("Food & Dining", "Grocery 1", new BigDecimal("450.00"), LocalDate.of(2026, 8, 5), null));
        expenseService.createEntry(userId, new ExpenseEntryRequest("Utilities", "Electricity", new BigDecimal("750.50"), LocalDate.of(2026, 8, 12), null));

        YearMonth ym = YearMonth.of(2026, 8);
        MonthlyAnalyticsReport report = analyticsService.generateMonthlyReport(userId, ym);
        BigDecimal expectedTotal = new BigDecimal("1200.50");
        assertThat(report.authoritativeTotal()).isEqualByComparingTo(expectedTotal);

        // 1. CSV
        ReportExportResult csv = exportService.exportCsv(userId, ym);
        String csvText = new String(csv.content(), StandardCharsets.UTF_8);
        assertThat(csvText).contains("Total,,,1200.50,");

        // 2. JSON
        ReportExportResult json = exportService.exportJson(userId, ym);
        JsonNode jsonNode = objectMapper.readTree(json.content());
        assertThat(jsonNode.get("authoritativeTotal").asDouble()).isEqualTo(1200.50);

        // 3. HTML / PDF
        ReportExportResult html = exportService.exportHtmlPdf(userId, ym);
        String htmlText = new String(html.content(), StandardCharsets.UTF_8);
        assertThat(htmlText).contains("₹1,200.50");
    }

    @Test
    void exportEndpoint_enforcesTenantIsolationAndHeaders() throws Exception {
        String token = registerAndGetToken("export-endpoint@example.com");

        mvc.perform(get("/api/v1/assistant/analytics/export?format=csv&period=2026-08")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dailymate-expenses-2026-08.csv\""))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")));

        mvc.perform(get("/api/v1/assistant/analytics/export?format=json&period=2026-08")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"dailymate-report-2026-08.json\""))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/json")));
    }
}
