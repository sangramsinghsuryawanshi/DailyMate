package com.dailymate.assistant.security;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Final response-safety and redaction layer.
 * Positioned at the absolute boundary to scrub any sensitive credentials, tokens, hashes,
 * or connection details before response persistence and API delivery.
 */
@Component
public class AssistantResponseRedactor {

    // JWT token pattern (header.payload.signature)
    private static final Pattern JWT_PATTERN = Pattern.compile(
            "eyJ[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+"
    );

    // Bearer token pattern
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "(?i)Bearer\\s+[A-Za-z0-9\\-_.]+"
    );

    // BCrypt password hash pattern
    private static final Pattern BCRYPT_PATTERN = Pattern.compile(
            "\\$2[aby]\\$[0-9]{2}\\$[A-Za-z0-9./]{53}"
    );

    // Key-value secret patterns (e.g. password=..., secret=..., api_key=...)
    private static final Pattern KEY_VALUE_SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(password|secret|api_key|token|jwt_secret|app_jwt_secret)=([^&\\s\\r\\n]+)"
    );

    // Database connection URL credentials
    private static final Pattern JDBC_CREDENTIAL_PATTERN = Pattern.compile(
            "(?i)(jdbc:[a-z0-9:]+://[^:]+:)([^@\\s]+)(@)"
    );

    // Stack trace patterns
    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile(
            "(?i)\\bat com\\.dailymate\\.[a-zA-Z0-9_.]+\\([^)]*\\)"
    );

    public String redact(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }

        String scrubbed = response;

        // 1. Scrub JWT tokens
        scrubbed = JWT_PATTERN.matcher(scrubbed).replaceAll("[REDACTED_JWT]");

        // 2. Scrub Bearer tokens
        scrubbed = BEARER_PATTERN.matcher(scrubbed).replaceAll("Bearer [REDACTED]");

        // 3. Scrub BCrypt password hashes
        scrubbed = BCRYPT_PATTERN.matcher(scrubbed).replaceAll("[REDACTED_HASH]");

        // 4. Scrub key-value secrets
        scrubbed = KEY_VALUE_SECRET_PATTERN.matcher(scrubbed).replaceAll("$1=[REDACTED]");

        // 5. Scrub JDBC credentials
        scrubbed = JDBC_CREDENTIAL_PATTERN.matcher(scrubbed).replaceAll("$1[REDACTED]$3");

        // 6. Scrub stack traces
        scrubbed = STACK_TRACE_PATTERN.matcher(scrubbed).replaceAll("[REDACTED_STACK]");

        return scrubbed;
    }
}
