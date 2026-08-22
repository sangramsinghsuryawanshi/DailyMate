package com.dailymate.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.entity.AssistantAction;
import com.dailymate.assistant.entity.AssistantActionStatus;
import com.dailymate.assistant.repository.AssistantActionRepository;
import com.dailymate.assistant.security.AssistantRateLimiter;
import com.dailymate.assistant.service.AssistantActionCleanupService;
import com.dailymate.assistant.service.AssistantActionService;
import com.dailymate.auth.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
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
class AssistantActionObservabilityIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AssistantActionRepository actionRepository;

    @Autowired
    private AssistantActionService actionService;

    @Autowired
    private AssistantActionCleanupService cleanupService;

    @Autowired
    private AssistantRateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        rateLimiter.reset();
    }

    private String registerAndGetToken(String email) throws Exception {
        String tokenBody = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "StrongPass123!", "Obs", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tokenBody).get("accessToken").asText();
    }

    @Test
    void persistsFailedStateWhenDomainMutationFails() throws Exception {
        String token = registerAndGetToken("obs-failure@example.com");

        // Manually propose an action with invalid parameters (e.g. negative amount which domain validation rejects)
        String userId = "00000000-0000-0000-0000-000000000001"; // arbitrary test userId
        var proposal = actionService.createProposal(
                userId,
                "RECORD_EXPENSE",
                "Invalid parameters",
                "NOT_VALID_JSON"
        );

        // Confirm action -> should throw BadRequestException
        try {
            actionService.confirmAction(userId, proposal.actionId(), new AssistantActionExecutionRequest("fail-key"));
        } catch (Exception ignored) {
            // Expected failure
        }

        // Verify action status in database is persisted as FAILED
        AssistantAction action = actionRepository.findById(proposal.actionId()).orElseThrow();
        assertThat(action.getStatus()).isEqualTo(AssistantActionStatus.FAILED);
        assertThat(action.getResultMessage()).contains("Execution failed");
    }

    @Test
    void cleanupPurgesOnlyStaleTerminalActionsAndRetainsActiveOnes() throws Exception {
        Instant now = Instant.now();
        Instant oldTime = now.minus(10, ChronoUnit.DAYS);

        // 1. Stale EXPIRED action
        AssistantAction expiredAction = new AssistantAction();
        expiredAction.setUserId("user-1");
        expiredAction.setActionType("RECORD_EXPENSE");
        expiredAction.setSummary("Expired");
        expiredAction.setParametersJson("{}");
        expiredAction.setStatus(AssistantActionStatus.EXPIRED);
        expiredAction.setExpiresAt(oldTime);
        actionRepository.save(expiredAction);

        // 2. Stale CANCELLED action
        AssistantAction cancelledAction = new AssistantAction();
        cancelledAction.setUserId("user-1");
        cancelledAction.setActionType("RECORD_EXPENSE");
        cancelledAction.setSummary("Cancelled");
        cancelledAction.setParametersJson("{}");
        cancelledAction.setStatus(AssistantActionStatus.CANCELLED);
        cancelledAction.setExpiresAt(oldTime);
        actionRepository.save(cancelledAction);

        // 3. Stale EXECUTED action (>7 days old)
        AssistantAction oldExecutedAction = new AssistantAction();
        oldExecutedAction.setUserId("user-1");
        oldExecutedAction.setActionType("RECORD_EXPENSE");
        oldExecutedAction.setSummary("Old Executed");
        oldExecutedAction.setParametersJson("{}");
        oldExecutedAction.setStatus(AssistantActionStatus.EXECUTED);
        oldExecutedAction.setExpiresAt(oldTime);
        oldExecutedAction.setExecutedAt(oldTime);
        actionRepository.save(oldExecutedAction);

        // 4. Recent EXECUTED action (within 7-day replay window)
        AssistantAction recentExecutedAction = new AssistantAction();
        recentExecutedAction.setUserId("user-1");
        recentExecutedAction.setActionType("RECORD_EXPENSE");
        recentExecutedAction.setSummary("Recent Executed");
        recentExecutedAction.setParametersJson("{}");
        recentExecutedAction.setStatus(AssistantActionStatus.EXECUTED);
        recentExecutedAction.setExpiresAt(now.plus(1, ChronoUnit.HOURS));
        recentExecutedAction.setExecutedAt(now.minus(1, ChronoUnit.DAYS));
        actionRepository.save(recentExecutedAction);

        // 5. Active PENDING action
        AssistantAction pendingAction = new AssistantAction();
        pendingAction.setUserId("user-1");
        pendingAction.setActionType("RECORD_EXPENSE");
        pendingAction.setSummary("Active Pending");
        pendingAction.setParametersJson("{}");
        pendingAction.setStatus(AssistantActionStatus.PENDING);
        pendingAction.setExpiresAt(now.plus(10, ChronoUnit.MINUTES));
        actionRepository.save(pendingAction);

        // Run cleanup targeting cutoff = 7 days ago
        Instant cutoff = now.minus(7, ChronoUnit.DAYS);
        int purged = cleanupService.purgeAllStaleActions(cutoff, cutoff);

        assertThat(purged).isGreaterThanOrEqualTo(3);
        assertThat(actionRepository.findById(expiredAction.getId())).isEmpty();
        assertThat(actionRepository.findById(cancelledAction.getId())).isEmpty();
        assertThat(actionRepository.findById(oldExecutedAction.getId())).isEmpty();

        // Verify recent executed and pending actions remain intact
        assertThat(actionRepository.findById(recentExecutedAction.getId())).isPresent();
        assertThat(actionRepository.findById(pendingAction.getId())).isPresent();
    }

    @Test
    void fullActionObservabilityFlow() throws Exception {
        String token = registerAndGetToken("obs-telemetry@example.com");

        // 1. Propose action -> PROPOSED
        String chatBody = mvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantChatRequest("Add expense 450 for Travel"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String actionId = objectMapper.readTree(chatBody).get("proposedAction").get("actionId").asText();

        // 2. Execute action -> EXECUTED
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("obs-key-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // 3. Replay action -> REPLAYED
        mvc.perform(post("/api/v1/assistant/actions/{id}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssistantActionExecutionRequest("obs-key-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));
    }
}
