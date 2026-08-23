package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.dailymate.assistant.controller.AssistantController;
import com.dailymate.assistant.service.AssistantActionService;
import com.dailymate.assistant.service.AssistantAnalyticsService;
import com.dailymate.assistant.service.AssistantBulkOperationsService;
import com.dailymate.assistant.service.AssistantContextService;
import com.dailymate.assistant.service.AssistantConversationStateManager;
import com.dailymate.assistant.service.AssistantExportService;
import com.dailymate.assistant.service.AssistantGroundingEngine;
import com.dailymate.assistant.service.AssistantReportingService;
import com.dailymate.assistant.service.AssistantService;
import com.dailymate.assistant.service.AssistantToolRouter;
import com.dailymate.assistant.tool.AssistantActionDispatcher;
import com.dailymate.assistant.tool.AssistantToolRegistry;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Permanent Architecture Invariant Test:
 * Enforces that no Assistant component directly accesses EntityManager, JdbcTemplate, or domain JpaRepositories.
 * All domain mutations must strictly route through authorized domain services.
 */
class AssistantGovernanceBoundaryArchitectureTest {

    private static final List<Class<?>> ASSISTANT_CLASSES = List.of(
            AssistantController.class,
            AssistantService.class,
            AssistantActionDispatcher.class,
            AssistantToolRouter.class,
            AssistantGroundingEngine.class,
            AssistantContextService.class,
            AssistantReportingService.class,
            AssistantBulkOperationsService.class,
            AssistantConversationStateManager.class,
            AssistantAnalyticsService.class,
            AssistantExportService.class,
            com.dailymate.assistant.controller.AssistantAnalyticsController.class,
            AssistantToolRegistry.class
    );

    @Test
    void verifiesZeroDirectDomainRepositoriesOrDirectPersistenceInAssistantLayer() {
        for (Class<?> clazz : ASSISTANT_CLASSES) {
            for (Field field : clazz.getDeclaredFields()) {
                String typeName = field.getType().getSimpleName();
                String typeFullName = field.getType().getName();

                // Disallow direct EntityManager or JdbcTemplate
                if (typeName.equals("EntityManager") || typeName.equals("JdbcTemplate") || typeName.equals("NamedParameterJdbcTemplate")) {
                    org.junit.jupiter.api.Assertions.fail(
                            "Architecture Invariant Violated: " + clazz.getSimpleName() + " must not directly use " + typeName
                    );
                }

                // Disallow domain entity repositories (only internal assistant repositories allowed)
                if (typeName.endsWith("Repository") && !typeName.startsWith("Assistant")) {
                    org.junit.jupiter.api.Assertions.fail(
                            "Architecture Invariant Violated: " + clazz.getSimpleName() + " must not directly reference domain repository: " + typeName
                    );
                }
            }
        }
    }
}
