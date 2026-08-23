package com.dailymate.assistant.domain;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Server-authoritative relative and natural date resolver for DailyMate AI Assistant.
 * Invariant: Dates are resolved exclusively using the authoritative Clock bean.
 */
@Component
public class AssistantDateResolver {

    private final Clock clock;

    private static final Pattern MONTH_NAME_PATTERN = Pattern.compile(
            "(?i)(?:on\\s+)?(?:(\\d{1,2})(?:st|nd|rd|th)?\\s+)?(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)(?:\\s+(\\d{1,2})(?:st|nd|rd|th)?)?(?:\\s*,?\\s*(\\d{4}))?"
    );

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile(
            "(?i)(?:on\\s+)?(\\d{4})-(\\d{2})-(\\d{2})"
    );

    private static final Pattern DMY_DATE_PATTERN = Pattern.compile(
            "(?i)(?:on\\s+)?(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})"
    );

    private static final Pattern DAY_OF_WEEK_PATTERN = Pattern.compile(
            "(?i)(?:on\\s+)?(last|this|next|coming)?\\s*(monday|tuesday|wednesday|thursday|friday|saturday|sunday)"
    );

    public AssistantDateResolver(Clock clock) {
        this.clock = clock;
    }

    public LocalDate getToday() {
        return LocalDate.now(clock);
    }

    /**
     * Resolves a raw date expression (e.g. "today", "yesterday", "on August 17", "last Monday")
     * into an authoritative LocalDate.
     */
    public Optional<LocalDate> resolveDate(String expression) {
        if (expression == null || expression.isBlank()) {
            return Optional.empty();
        }

        String normalized = expression.trim().toLowerCase(Locale.ROOT)
                .replaceAll("^on\\s+", "")
                .replaceAll("[.,;]+$", "")
                .trim();

        LocalDate today = LocalDate.now(clock);

        switch (normalized) {
            case "today" -> { return Optional.of(today); }
            case "yesterday" -> { return Optional.of(today.minusDays(1)); }
            case "tomorrow" -> { return Optional.of(today.plusDays(1)); }
            case "day before yesterday" -> { return Optional.of(today.minusDays(2)); }
            case "day after tomorrow" -> { return Optional.of(today.plusDays(2)); }
        }

        // 1. ISO Date check (2026-08-17)
        Matcher isoMatcher = ISO_DATE_PATTERN.matcher(normalized);
        if (isoMatcher.matches()) {
            try {
                return Optional.of(LocalDate.of(
                        Integer.parseInt(isoMatcher.group(1)),
                        Integer.parseInt(isoMatcher.group(2)),
                        Integer.parseInt(isoMatcher.group(3))
                ));
            } catch (Exception ignored) {}
        }

        // 2. DMY Date check (17/08/2026)
        Matcher dmyMatcher = DMY_DATE_PATTERN.matcher(normalized);
        if (dmyMatcher.matches()) {
            try {
                return Optional.of(LocalDate.of(
                        Integer.parseInt(dmyMatcher.group(3)),
                        Integer.parseInt(dmyMatcher.group(2)),
                        Integer.parseInt(dmyMatcher.group(1))
                ));
            } catch (Exception ignored) {}
        }

        // 3. Month Name check ("August 17", "17th August", "Aug 17", "17 August 2026")
        Matcher monthMatcher = MONTH_NAME_PATTERN.matcher(normalized);
        if (monthMatcher.matches()) {
            try {
                String dayBefore = monthMatcher.group(1);
                String monthStr = monthMatcher.group(2).toUpperCase(Locale.ROOT);
                String dayAfter = monthMatcher.group(3);
                String yearStr = monthMatcher.group(4);

                int day = dayBefore != null ? Integer.parseInt(dayBefore) : (dayAfter != null ? Integer.parseInt(dayAfter) : 1);
                Month month = parseMonth(monthStr);
                int year = yearStr != null ? Integer.parseInt(yearStr) : today.getYear();

                return Optional.of(LocalDate.of(year, month, day));
            } catch (Exception ignored) {}
        }

        // 4. Day of Week check ("last Monday", "this Friday", "next Sunday")
        Matcher dowMatcher = DAY_OF_WEEK_PATTERN.matcher(normalized);
        if (dowMatcher.matches()) {
            try {
                String modifier = dowMatcher.group(1);
                String dowStr = dowMatcher.group(2).toUpperCase(Locale.ROOT);
                DayOfWeek dow = DayOfWeek.valueOf(dowStr);

                if ("last".equalsIgnoreCase(modifier)) {
                    return Optional.of(today.with(TemporalAdjusters.previous(dow)));
                } else if ("next".equalsIgnoreCase(modifier) || "coming".equalsIgnoreCase(modifier)) {
                    return Optional.of(today.with(TemporalAdjusters.next(dow)));
                } else { // "this" or unspecified
                    if (today.getDayOfWeek() == dow) {
                        return Optional.of(today);
                    }
                    return Optional.of(today.with(TemporalAdjusters.nextOrSame(dow)));
                }
            } catch (Exception ignored) {}
        }

        return Optional.empty();
    }

    private Month parseMonth(String monthStr) {
        String clean = monthStr.toUpperCase(Locale.ROOT);
        return switch (clean) {
            case "JAN", "JANUARY" -> Month.JANUARY;
            case "FEB", "FEBRUARY" -> Month.FEBRUARY;
            case "MAR", "MARCH" -> Month.MARCH;
            case "APR", "APRIL" -> Month.APRIL;
            case "MAY" -> Month.MAY;
            case "JUN", "JUNE" -> Month.JUNE;
            case "JUL", "JULY" -> Month.JULY;
            case "AUG", "AUGUST" -> Month.AUGUST;
            case "SEP", "SEPT", "SEPTEMBER" -> Month.SEPTEMBER;
            case "OCT", "OCTOBER" -> Month.OCTOBER;
            case "NOV", "NOVEMBER" -> Month.NOVEMBER;
            case "DEC", "DECEMBER" -> Month.DECEMBER;
            default -> Month.valueOf(clean);
        };
    }
}
