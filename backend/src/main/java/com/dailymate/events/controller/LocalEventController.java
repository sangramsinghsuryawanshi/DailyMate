package com.dailymate.events.controller;

import com.dailymate.events.dto.request.LocalEventRequest;
import com.dailymate.events.dto.response.LocalEventResponse;
import com.dailymate.events.service.LocalEventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class LocalEventController {

    private final LocalEventService localEventService;

    public LocalEventController(LocalEventService localEventService) {
        this.localEventService = localEventService;
    }

    @GetMapping("/events")
    public List<LocalEventResponse> getEvents() {
        return localEventService.getEvents();
    }

    @PostMapping("/events")
    public ResponseEntity<LocalEventResponse> createEvent(@Valid @RequestBody LocalEventRequest request) {
        LocalEventResponse response = localEventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/events/{id}")
    public LocalEventResponse updateEvent(@PathVariable String id, @Valid @RequestBody LocalEventRequest request) {
        return localEventService.updateEvent(id, request);
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        localEventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
