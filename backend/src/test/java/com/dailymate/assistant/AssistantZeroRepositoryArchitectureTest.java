package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.dailymate.assistant.service.AssistantActionService;
import com.dailymate.assistant.service.AssistantContextService;
import com.dailymate.assistant.service.AssistantGroundingEngine;
import com.dailymate.assistant.service.AssistantReportingService;
import com.dailymate.assistant.service.AssistantService;
import com.dailymate.assistant.service.AssistantToolRouter;
import com.dailymate.assistant.tool.AssistantActionDispatcher;
import com.dailymate.assistant.tool.AssistantToolRegistry;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssistantZeroRepositoryArchitectureTest {

    @Test
    void verifiesAssistantExecutionLayerHasZeroDirectDomainRepositories() {
        // Assert AssistantActionDispatcher has zero domain repositories (only domain services)
        Field[] fields = AssistantActionDispatcher.class.getDeclaredFields();
        for (Field f : fields) {
            String typeName = f.getType().getSimpleName();
            if (typeName.endsWith("Repository")) {
                org.junit.jupiter.api.Assertions.fail(
                        "Architecture Invariant Violated: AssistantActionDispatcher must not directly reference repositories: " + typeName
                );
            }
        }

        // Assert AssistantToolRouter has zero domain repositories
        for (Field f : AssistantToolRouter.class.getDeclaredFields()) {
            String typeName = f.getType().getSimpleName();
            if (typeName.endsWith("Repository")) {
                org.junit.jupiter.api.Assertions.fail(
                        "Architecture Invariant Violated: AssistantToolRouter must not directly reference repositories: " + typeName
                );
            }
        }

        // Assert AssistantGroundingEngine has zero domain repositories
        for (Field f : AssistantGroundingEngine.class.getDeclaredFields()) {
            String typeName = f.getType().getSimpleName();
            if (typeName.endsWith("Repository")) {
                org.junit.jupiter.api.Assertions.fail(
                        "Architecture Invariant Violated: AssistantGroundingEngine must not directly reference repositories: " + typeName
                );
            }
        }
    }
}
