package com.dailymate.events.controller;

import com.dailymate.core.security.UserPrincipal;
import com.dailymate.events.dto.request.LocalEventCreateRequest;
import com.dailymate.events.dto.request.LocalEventUpdateRequest;
import com.dailymate.events.dto.response.LocalEventResponse;
import com.dailymate.events.service.LocalEventService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class LocalEventController {

    private final LocalEventService localEventService;

    public LocalEventController(LocalEventService localEventService) {
        this.localEventService = localEventService;
    }

    @GetMapping("/events")
    public List<LocalEventResponse> getEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return localEventService.getEvents(category, status);
    }

    @GetMapping("/my-events")
    public List<LocalEventResponse> getMyEvents(@AuthenticationPrincipal UserPrincipal principal) {
        return localEventService.getMyEvents(principal.user().getId());
    }

    @PostMapping("/events")
    public ResponseEntity<LocalEventResponse> createEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LocalEventCreateRequest request) {
        LocalEventResponse response = localEventService.createEvent(principal.user().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/events/{id}")
    public LocalEventResponse updateEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody LocalEventUpdateRequest request) {
        return localEventService.updateEvent(principal.user().getId(), id, request);
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        localEventService.deleteEvent(principal.user().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
