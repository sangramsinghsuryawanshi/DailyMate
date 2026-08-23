package com.dailymate.assistant.controller;

import com.dailymate.assistant.dto.AssistantAnalyticsDto.*;
import com.dailymate.assistant.service.AssistantAnalyticsService;
import com.dailymate.assistant.service.AssistantExportService;
import com.dailymate.core.security.UserPrincipal;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant/analytics")
public class AssistantAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger("ASSISTANT_EXPORT_AUDIT");

    private final AssistantAnalyticsService analyticsService;
    private final AssistantExportService exportService;

    public AssistantAnalyticsController(
            AssistantAnalyticsService analyticsService,
            AssistantExportService exportService) {
        this.analyticsService = analyticsService;
        this.exportService = exportService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyAnalyticsReport> getMonthlyReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String period) {
        YearMonth ym = parseYearMonth(period);
        MonthlyAnalyticsReport report = analyticsService.generateMonthlyReport(principal.user().getId(), ym);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/compare")
    public ResponseEntity<PeriodComparisonDto> comparePeriods(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String period1,
            @RequestParam String period2) {
        YearMonth ym1 = YearMonth.parse(period1);
        YearMonth ym2 = YearMonth.parse(period2);
        PeriodComparisonDto comparison = analyticsService.comparePeriods(principal.user().getId(), ym1, ym2);
        return ResponseEntity.ok(comparison);
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategoryBreakdown(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String period) {
        YearMonth ym = parseYearMonth(period);
        var breakdown = analyticsService.getCategoryBreakdown(principal.user().getId(), ym);
        return ResponseEntity.ok(breakdown);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String period) {
        YearMonth ym = parseYearMonth(period);
        ReportExportResult result;

        if ("json".equalsIgnoreCase(format)) {
            result = exportService.exportJson(principal.user().getId(), ym);
        } else if ("pdf".equalsIgnoreCase(format) || "html".equalsIgnoreCase(format)) {
            result = exportService.exportHtmlPdf(principal.user().getId(), ym);
        } else {
            result = exportService.exportCsv(principal.user().getId(), ym);
        }

        log.info("ASSISTANT_EXPORT actorId={} format={} period={} filename={} bytes={}",
                principal.user().getId(), format, ym, result.filename(), result.content().length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }

    private YearMonth parseYearMonth(String period) {
        if (period == null || period.isBlank()) {
            return YearMonth.now(AssistantAnalyticsService.DEFAULT_ZONE);
        }
        return YearMonth.parse(period.trim());
    }
}
