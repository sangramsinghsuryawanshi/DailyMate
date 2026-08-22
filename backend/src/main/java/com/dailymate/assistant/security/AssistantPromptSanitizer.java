package com.dailymate.assistant.security;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Early detection and fast-rejection filter for blatant adversarial prompt injection,
 * role impersonation, instruction smuggling, and privilege escalation attempts.
 *
 * Prevents adversarial prompts from triggering private tenant database context lookups.
 */
@Component
public class AssistantPromptSanitizer {

    private static final Pattern DELIMITER_PATTERN = Pattern.compile(
            "(\\[INST\\]|<<SYS>>|--- BEGIN SYSTEM|\\b(system|developer|admin):)",
            Pattern.CASE_INSENSITIVE
    );

    public boolean isAdversarialOrRestricted(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return false;
        }

        String lower = prompt.toLowerCase(Locale.ROOT).trim();

        // 1. Delimiter & instruction smuggling
        if (DELIMITER_PATTERN.matcher(prompt).find()) {
            return true;
        }

        // 2. System override / jailbreak patterns
        if (lower.contains("ignore prior instruction")
                || lower.contains("ignore previous instruction")
                || lower.contains("ignore all instruction")
                || lower.contains("disregard all previous")
                || lower.contains("bypass security")
                || lower.contains("admin override")) {
            return true;
        }

        // 3. Credential / database exfiltration attempts
        if (lower.contains("all password")
                || lower.contains("database schema")
                || lower.contains("database credential")
                || lower.contains("show connection string")
                || lower.contains("jwt secret")
                || lower.contains("all user tokens")
                || lower.contains("list all users")) {
            return true;
        }

        return false;
    }

    public String getSafeRejectionMessage() {
        return "Administrative operations, system overrides, and private user records are strictly protected and cannot be accessed through the assistant.";
    }
}
