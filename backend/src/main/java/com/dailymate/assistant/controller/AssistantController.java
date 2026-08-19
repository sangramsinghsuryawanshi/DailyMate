package com.dailymate.assistant.controller;

import com.dailymate.assistant.dto.request.AssistantConversationRequest;
import com.dailymate.assistant.dto.response.AssistantConversationResponse;
import com.dailymate.assistant.service.AssistantService;
import com.dailymate.core.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @GetMapping("/conversations")
    public List<AssistantConversationResponse> getConversations(@AuthenticationPrincipal UserPrincipal principal) {
        return assistantService.getConversations(principal.user().getId());
    }

    @PostMapping("/conversations")
    public ResponseEntity<AssistantConversationResponse> createConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssistantConversationRequest request) {
        AssistantConversationResponse response = assistantService.createConversation(principal.user().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/conversations/{id}")
    public AssistantConversationResponse updateConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody AssistantConversationRequest request) {
        return assistantService.updateConversation(principal.user().getId(), id, request);
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String id) {
        assistantService.deleteConversation(principal.user().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
