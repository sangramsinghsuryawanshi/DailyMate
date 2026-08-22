package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.AssistantContext;
import org.springframework.stereotype.Service;

@Service
public class AssistantGroundingEngine {

    private final AssistantToolRouter toolRouter;

    public AssistantGroundingEngine(AssistantToolRouter toolRouter) {
        this.toolRouter = toolRouter;
    }

    public record ActionProposalData(
            String actionType,
            String summary,
            String parametersJson) {
    }

    public record GroundingResult(
            String textResponse,
            ActionProposalData proposal) {
    }

    public GroundingResult process(String prompt, String userId, AssistantContext context) {
        return toolRouter.route(prompt, userId, context);
    }
}
