package com.dailymate.events.service;

import com.dailymate.core.dto.response.PageResponse;
import com.dailymate.core.exception.BadRequestException;
import com.dailymate.core.exception.NotFoundException;
import com.dailymate.core.util.PaginationUtils;
import com.dailymate.events.dto.request.LocalEventCreateRequest;
import com.dailymate.events.dto.request.LocalEventUpdateRequest;
import com.dailymate.events.dto.response.LocalEventResponse;
import com.dailymate.events.entity.EventStatus;
import com.dailymate.events.entity.LocalEvent;
import com.dailymate.events.repository.LocalEventRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalEventService {

    private final LocalEventRepository events;

    public LocalEventService(LocalEventRepository events) {
        this.events = events;
    }

    public PageResponse<LocalEventResponse> getEvents(String category, String status, int page, int size) {
        String normalizedCategory = (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL"))
                ? category.trim()
                : null;
        String normalizedStatus = (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL"))
                ? status.trim().toUpperCase()
                : null;

        Sort sort = Sort.by(Sort.Order.asc("eventDate"), Sort.Order.asc("id"));
        Pageable pageable = PaginationUtils.createPageable(page, size, sort);
        Page<LocalEventResponse> responsePage = events.findFiltered(normalizedCategory, normalizedStatus, pageable)
                .map(this::toResponse);
        return PageResponse.from(responsePage);
    }

    public List<LocalEventResponse> getEvents(String category, String status) {
        String normalizedCategory = (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL"))
                ? category.trim()
                : null;
        String normalizedStatus = (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL"))
                ? status.trim().toUpperCase()
                : null;

        List<LocalEvent> result;
        if (normalizedCategory != null && normalizedStatus != null) {
            result = events.findAllByCategoryAndStatusOrderByEventDateAsc(normalizedCategory, normalizedStatus);
        } else if (normalizedCategory != null) {
            result = events.findAllByCategoryOrderByEventDateAsc(normalizedCategory);
        } else if (normalizedStatus != null) {
            result = events.findAllByStatusOrderByEventDateAsc(normalizedStatus);
        } else {
            result = events.findAllByOrderByEventDateAsc();
        }

        return result.stream().map(this::toResponse).toList();
    }

    public PageResponse<LocalEventResponse> getMyEvents(String userId, int page, int size) {
        Sort sort = Sort.by(Sort.Order.asc("eventDate"), Sort.Order.asc("id"));
        Pageable pageable = PaginationUtils.createPageable(page, size, sort);
        Page<LocalEventResponse> responsePage = events.findByUserId(userId, pageable)
                .map(this::toResponse);
        return PageResponse.from(responsePage);
    }

    public List<LocalEventResponse> getMyEvents(String userId) {
        return events.findAllByUserIdOrderByEventDateAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LocalEventResponse createEvent(String userId, LocalEventCreateRequest request) {
        LocalEvent event = new LocalEvent();
        event.setUserId(userId);
        event.setTitle(request.title().trim());
        event.setCategory(request.category().trim());
        event.setLocation(request.location().trim());
        event.setEventDate(request.eventDate());
        event.setDescription(request.description().trim());
        event.setStatus(EventStatus.PUBLISHED.name());

        return toResponse(events.save(event));
    }

    @Transactional
    public LocalEventResponse updateEvent(String userId, String eventId, LocalEventUpdateRequest request) {
        LocalEvent event = events.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        EventStatus targetStatus = EventStatus.fromString(request.status());
        if (targetStatus == null) {
            throw new BadRequestException("Invalid event status: " + request.status());
        }

        event.setTitle(request.title().trim());
        event.setCategory(request.category().trim());
        event.setLocation(request.location().trim());
        event.setEventDate(request.eventDate());
        event.setDescription(request.description().trim());
        event.setStatus(targetStatus.name());

        return toResponse(events.save(event));
    }

    @Transactional
    public void deleteEvent(String userId, String eventId) {
        LocalEvent event = events.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        events.delete(event);
    }

    private LocalEventResponse toResponse(LocalEvent event) {
        return new LocalEventResponse(
                event.getId(),
                event.getUserId(),
                event.getTitle(),
                event.getCategory(),
                event.getLocation(),
                event.getEventDate(),
                event.getDescription(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
