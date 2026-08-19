package com.dailymate.notification.controller;

import com.dailymate.core.security.UserPrincipal;
import com.dailymate.notification.dto.request.NotificationRequest;
import com.dailymate.notification.dto.response.NotificationResponse;
import com.dailymate.notification.service.NotificationService;
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
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public com.dailymate.notification.dto.response.NotificationPageResponse getNotifications(@AuthenticationPrincipal UserPrincipal principal,
                                                                                                  @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
                                                                                                  @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {
        return notificationService.getNotificationsPaged(principal.user().getId(), page, size);
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = notificationService.createNotification(principal.user().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public NotificationResponse updateNotification(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody NotificationRequest request) {
        return notificationService.updateNotification(principal.user().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String id) {
        notificationService.deleteNotification(principal.user().getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.user().getId());
        return ResponseEntity.noContent().build();
    }
}
