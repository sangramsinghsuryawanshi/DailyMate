package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.AssistantAnalyticsDto.*;
import com.dailymate.expense.dto.response.ExpenseEntryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Deterministic Export Service for DailyMate AI Assistant.
 * Generates RFC-4180 compliant CSV, structured JSON, and formatted PDF-ready HTML.
 * Invariant: All export formats contain identical authoritative totals computed by AssistantAnalyticsService.
 */
@Service
public class AssistantExportService {

    private final AssistantAnalyticsService analyticsService;
    private final ObjectMapper objectMapper;
    private final DecimalFormat inrFormat = new DecimalFormat("₹#,##,##0.00");

    public AssistantExportService(AssistantAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public ReportExportResult exportCsv(String userId, YearMonth period) {
        MonthlyAnalyticsReport report = analyticsService.generateMonthlyReport(userId, period);
        List<ExpenseEntryResponse> entries = analyticsService.getEntriesForPeriod(userId, period);

        StringBuilder sb = new StringBuilder();
        // Deterministic CSV Header
        sb.append("Date,Category,Description,Amount (INR),Notes\n");

        for (ExpenseEntryResponse entry : entries) {
            sb.append(escapeCsv(entry.spentOn() != null ? entry.spentOn().toString() : ""))
                    .append(",")
                    .append(escapeCsv(entry.category()))
                    .append(",")
                    .append(escapeCsv(entry.description()))
                    .append(",")
                    .append(entry.amount() != null ? entry.amount().toPlainString() : "0.00")
                    .append(",")
                    .append(escapeCsv(entry.notes() != null ? entry.notes() : ""))
                    .append("\n");
        }

        // Authoritative Total Row
        sb.append("Total,,,")
                .append(report.authoritativeTotal().toPlainString())
                .append(",\n");

        String filename = "dailymate-expenses-" + period + ".csv";
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return new ReportExportResult(filename, "text/csv; charset=UTF-8", bytes);
    }

    public ReportExportResult exportJson(String userId, YearMonth period) {
        MonthlyAnalyticsReport report = analyticsService.generateMonthlyReport(userId, period);
        List<ExpenseEntryResponse> entries = analyticsService.getEntriesForPeriod(userId, period);

        Map<String, Object> data = new HashMap<>();
        data.put("report", report);
        data.put("itemizedExpenses", entries);
        data.put("authoritativeTotal", report.authoritativeTotal());

        try {
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
            String filename = "dailymate-report-" + period + ".json";
            return new ReportExportResult(filename, "application/json", bytes);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize analytics report to JSON", ex);
        }
    }

    public ReportExportResult exportHtmlPdf(String userId, YearMonth period) {
        MonthlyAnalyticsReport report = analyticsService.generateMonthlyReport(userId, period);
        List<ExpenseEntryResponse> entries = analyticsService.getEntriesForPeriod(userId, period);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<title>DailyMate Life & Expense Report — ").append(period).append("</title>\n")
                .append("<style>\n")
                .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 24px; color: #1e293b; }\n")
                .append(".header { border-bottom: 2px solid #3b82f6; padding-bottom: 12px; margin-bottom: 20px; }\n")
                .append(".card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 16px; }\n")
                .append("table { width: 100%; border-collapse: collapse; margin-top: 12px; }\n")
                .append("th, td { border: 1px solid #cbd5e1; padding: 8px 12px; text-align: left; }\n")
                .append("th { background-color: #f1f5f9; }\n")
                .append(".total-row { font-weight: bold; background-color: #e2e8f0; }\n")
                .append("</style>\n</head>\n<body>\n")
                .append("<div class=\"header\"><h1>📊 DailyMate Monthly Life Report</h1><p>Period: <strong>").append(period).append("</strong></p></div>\n")
                .append("<div class=\"card\"><h2>Total Expenses: ").append(inrFormat.format(report.authoritativeTotal())).append("</h2>\n")
                .append("<p>Active Medicine Reminders: ").append(report.activeRemindersCount()).append(" | ICE Contacts: ").append(report.emergencyContactCount()).append("</p></div>\n")
                .append("<h3>Itemized Transactions</h3>\n")
                .append("<table>\n<thead><tr><th>Date</th><th>Category</th><th>Description</th><th>Amount</th></tr></thead>\n<tbody>\n");

        for (ExpenseEntryResponse e : entries) {
            html.append("<tr><td>").append(e.spentOn()).append("</td>")
                    .append("<td>").append(e.category()).append("</td>")
                    .append("<td>").append(e.description()).append("</td>")
                    .append("<td>").append(inrFormat.format(e.amount())).append("</td></tr>\n");
        }

        html.append("<tr class=\"total-row\"><td colspan=\"3\">Authoritative Total</td><td>")
                .append(inrFormat.format(report.authoritativeTotal())).append("</td></tr>\n")
                .append("</tbody>\n</table>\n</body>\n</html>");

        String filename = "dailymate-report-" + period + ".html";
        byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
        return new ReportExportResult(filename, "text/html; charset=UTF-8", bytes);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
