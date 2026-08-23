package com.dailymate.assistant.domain;

import com.dailymate.assistant.tool.ToolDomain;
import java.util.List;

/**
 * Server-authoritative structured extraction result for AI Assistant tool arguments.
 * Invariant: Prevents partially extracted or invalid objects from proceeding to preview/execution.
 */
public record ExtractionResult<T>(
        ToolDomain domain,
        String operation,
        boolean valid,
        List<T> items,
        List<String> errors,
        List<String> warnings) {

    public static <T> ExtractionResult<T> success(ToolDomain domain, String operation, List<T> items) {
        return new ExtractionResult<>(domain, operation, true, items != null ? items : List.of(), List.of(), List.of());
    }

    public static <T> ExtractionResult<T> successWithWarnings(ToolDomain domain, String operation, List<T> items, List<String> warnings) {
        return new ExtractionResult<>(domain, operation, true, items != null ? items : List.of(), List.of(), warnings != null ? warnings : List.of());
    }

    public static <T> ExtractionResult<T> failure(ToolDomain domain, String operation, List<String> errors) {
        return new ExtractionResult<>(domain, operation, false, List.of(), errors != null ? errors : List.of(), List.of());
    }

    public static <T> ExtractionResult<T> empty(ToolDomain domain, String operation) {
        return new ExtractionResult<>(domain, operation, false, List.of(), List.of("No valid items could be extracted."), List.of());
    }
}
