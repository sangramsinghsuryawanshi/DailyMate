package com.dailymate.assistant.controller;

import com.dailymate.assistant.dto.BulkExecutionResultDto;
import com.dailymate.assistant.dto.BulkOperationPreviewDto;
import com.dailymate.assistant.dto.request.AssistantActionExecutionRequest;
import com.dailymate.assistant.dto.request.AssistantChatRequest;
import com.dailymate.assistant.dto.request.BulkConfirmRequest;
import com.dailymate.assistant.dto.request.BulkPreviewRequest;
import com.dailymate.assistant.dto.response.AssistantActionExecutionResponse;
import com.dailymate.assistant.dto.response.AssistantChatResponse;
import com.dailymate.assistant.dto.response.AssistantConversationResponse;
import com.dailymate.assistant.service.AssistantBulkOperationsService;
import com.dailymate.assistant.service.AssistantService;
import com.dailymate.assistant.tool.AssistantToolDefinition;
import com.dailymate.assistant.tool.AssistantToolRegistry;
import com.dailymate.core.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantToolRegistry toolRegistry;
    private final AssistantBulkOperationsService bulkService;

    public AssistantController(
            AssistantService assistantService,
            AssistantToolRegistry toolRegistry,
            AssistantBulkOperationsService bulkService) {
        this.assistantService = assistantService;
        this.toolRegistry = toolRegistry;
        this.bulkService = bulkService;
    }

    @GetMapping("/tools")
    public Collection<AssistantToolDefinition> getTools(@AuthenticationPrincipal UserPrincipal principal) {
        return toolRegistry.getAllTools().values();
    }

    @GetMapping("/conversations")
    public List<AssistantConversationResponse> getConversations(@AuthenticationPrincipal UserPrincipal principal) {
        return assistantService.getConversations(principal.user().getId());
    }

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssistantChatRequest request) {
        AssistantChatResponse response = assistantService.chat(principal.user().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/actions/{id}/confirm")
    public ResponseEntity<AssistantActionExecutionResponse> confirmAction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @RequestBody(required = false) AssistantActionExecutionRequest request) {
        AssistantActionExecutionResponse response = assistantService.confirmAction(principal.user().getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/actions/{id}/cancel")
    public ResponseEntity<AssistantActionExecutionResponse> cancelAction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        AssistantActionExecutionResponse response = assistantService.cancelAction(principal.user().getId(), id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        assistantService.deleteConversation(principal.user().getId(), id);
        return ResponseEntity.noContent().build();
    }

    // --- Universal Bulk Operations Endpoints ---

    @PostMapping("/bulk/preview")
    public ResponseEntity<BulkOperationPreviewDto> previewBulkOperation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BulkPreviewRequest request) {
        String role = principal.user().getRole() != null ? principal.user().getRole().name() : "USER";
        BulkOperationPreviewDto preview = bulkService.previewBulkOperation(
                principal.user().getId(),
                role,
                request.toolName(),
                request.payloadRows()
        );
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/bulk/{bulkExecutionId}/confirm")
    public ResponseEntity<BulkExecutionResultDto> confirmBulkOperation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String bulkExecutionId,
            @RequestBody(required = false) BulkConfirmRequest request) {
        String role = principal.user().getRole() != null ? principal.user().getRole().name() : "USER";
        String previewHash = request != null ? request.previewHash() : null;
        BulkExecutionResultDto result = bulkService.confirmBulkOperation(
                principal.user().getId(),
                role,
                bulkExecutionId,
                previewHash
        );
        return ResponseEntity.ok(result);
    }
}
