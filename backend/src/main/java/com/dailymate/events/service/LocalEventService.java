package com.dailymate.events.service;

import com.dailymate.core.exception.NotFoundException;
import com.dailymate.events.dto.request.LocalEventRequest;
import com.dailymate.events.dto.response.LocalEventResponse;
import com.dailymate.events.entity.LocalEvent;
import com.dailymate.events.repository.LocalEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalEventService {

    private final LocalEventRepository events;

    public LocalEventService(LocalEventRepository events) {
        this.events = events;
    }

    public List<LocalEventResponse> getEvents() {
        return events.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public LocalEventResponse createEvent(LocalEventRequest request) {
        LocalEvent event = new LocalEvent();
        applyChanges(event, request);
        return toResponse(events.save(event));
    }

    @Transactional
    public LocalEventResponse updateEvent(String eventId, LocalEventRequest request) {
        LocalEvent event = findEvent(eventId);
        applyChanges(event, request);
        return toResponse(events.save(event));
    }

    @Transactional
    public void deleteEvent(String eventId) {
        LocalEvent event = findEvent(eventId);
        events.delete(event);
    }

    private void applyChanges(LocalEvent event, LocalEventRequest request) {
        event.setTitle(request.title().trim());
        event.setCategory(request.category().trim());
        event.setLocation(request.location().trim());
        event.setEventDate(request.eventDate());
        event.setDescription(request.description().trim());
    }

    private LocalEvent findEvent(String eventId) {
        return events.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    private LocalEventResponse toResponse(LocalEvent event) {
        return new LocalEventResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getLocation(),
                event.getEventDate(),
                event.getDescription(),
                event.getCreatedAt());
    }
}
