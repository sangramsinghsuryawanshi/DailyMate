package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dailymate.assistant.tool.AssistantToolDefinition;
import com.dailymate.assistant.tool.AssistantToolRegistry;
import com.dailymate.assistant.tool.AuthorizationPolicy;
import com.dailymate.assistant.tool.DataSensitivity;
import com.dailymate.assistant.tool.OperationPolicy;
import com.dailymate.assistant.tool.OperationScope;
import com.dailymate.assistant.tool.ResourceVisibility;
import com.dailymate.assistant.tool.TargetScope;
import com.dailymate.assistant.tool.ToolDomain;
import com.dailymate.assistant.tool.ToolOperationType;
import com.dailymate.assistant.tool.ToolRiskTier;
import com.dailymate.assistant.tool.ToolScope;
import com.dailymate.core.exception.ForbiddenException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssistantToolRegistryTest {

    private AssistantToolRegistry registry;

    @BeforeEach
    void setup() {
        registry = new AssistantToolRegistry();
    }

    @Test
    void validatesAllRegisteredToolsHaveUniqueNamesAndValidMetadata() {
        Map<String, AssistantToolDefinition> tools = registry.getAllTools();
        assertThat(tools).isNotEmpty();

        for (AssistantToolDefinition tool : tools.values()) {
            assertThat(tool.name()).isNotBlank();
            assertThat(tool.description()).isNotBlank();
            assertThat(tool.domain()).isNotNull();
            assertThat(tool.operationType()).isNotNull();
            assertThat(tool.riskTier()).isNotNull();
            assertThat(tool.scope()).isNotNull();
            assertThat(tool.operationScope()).isNotNull();
            assertThat(tool.allowedRoles()).isNotEmpty();

            if (tool.operationType() == ToolOperationType.READ || tool.operationType() == ToolOperationType.REPORT) {
                assertThat(tool.riskTier()).isEqualTo(ToolRiskTier.TIER_1);
            }

            if (tool.riskTier() == ToolRiskTier.TIER_3) {
                assertThat(tool.confirmationRequired()).isTrue();
                assertThat(tool.idempotencyRequired()).isTrue();
                assertThat(tool.auditRequired()).isTrue();
            }

            if (tool.destructive()) {
                assertThat(tool.riskTier()).isEqualTo(ToolRiskTier.TIER_3);
                assertThat(tool.confirmationRequired()).isTrue();
            }
        }
    }

    @Test
    void validatesServerAuthoritativeRoleGating() {
        // USER role allowed for standard tools
        registry.validateAuthorization("expense.record", "USER");
        registry.validateAuthorization("medicine.create", "ADMIN");

        // GUEST or invalid role denied
        assertThatThrownBy(() -> registry.validateAuthorization("expense.record", "GUEST"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void failFastRejectsTier3WithoutConfirmation() {
        AssistantToolRegistry customRegistry = new AssistantToolRegistry();
        AssistantToolDefinition invalidTool = new AssistantToolDefinition(
                "invalid.tier3",
                "Invalid tool definition",
                ToolDomain.EXPENSE,
                ToolOperationType.CREATE,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.USER_PRIVATE, TargetScope.OWNED_RESOURCES, Set.of("USER"), true, DataSensitivity.RESTRICTED),
                new OperationPolicy(ToolRiskTier.TIER_3, false, false, true, true, false, false, false, false), // invalid: confirmationRequired = false
                List.of("id"),
                List.of()
        );

        assertThatThrownBy(() -> customRegistry.register(invalidTool))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TIER_3 tool MUST require confirmation");
    }

    @Test
    void failFastRejectsReadOperationWithMutationTier() {
        AssistantToolRegistry customRegistry = new AssistantToolRegistry();
        AssistantToolDefinition invalidTool = new AssistantToolDefinition(
                "invalid.read",
                "Invalid read definition",
                ToolDomain.EXPENSE,
                ToolOperationType.READ,
                new AuthorizationPolicy(ToolScope.USER, OperationScope.SINGLE, ResourceVisibility.PUBLIC, TargetScope.PUBLIC_RESOURCES, Set.of("USER"), false, DataSensitivity.PUBLIC),
                new OperationPolicy(ToolRiskTier.TIER_3, false, false, false, false, false, false, false, false), // invalid: READ cannot be Tier 3
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> customRegistry.register(invalidTool))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("READ/REPORT operation must have TIER_1 risk classification");
    }
}
