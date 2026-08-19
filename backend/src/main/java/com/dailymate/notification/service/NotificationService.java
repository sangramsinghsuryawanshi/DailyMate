package com.dailymate.notification.service;

import com.dailymate.core.exception.NotFoundException;
import com.dailymate.notification.dto.request.NotificationRequest;
import com.dailymate.notification.dto.response.NotificationResponse;
import com.dailymate.notification.entity.Notification;
import com.dailymate.notification.repository.NotificationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    public List<NotificationResponse> getNotifications(String userId) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public com.dailymate.notification.dto.response.NotificationPageResponse getNotificationsPaged(String userId, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var paged = notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        var content = paged.getContent().stream().map(this::toResponse).toList();
        return new com.dailymate.notification.dto.response.NotificationPageResponse(content, paged.getTotalElements(), paged.getNumber(), paged.getSize());
    }

    public NotificationResponse createNotification(String userId, NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        applyChanges(notification, request);
        return toResponse(notifications.save(notification));
    }

    @Transactional
    public NotificationResponse updateNotification(String userId, String notificationId, NotificationRequest request) {
        Notification notification = findNotification(userId, notificationId);
        applyChanges(notification, request);
        return toResponse(notifications.save(notification));
    }

    @Transactional
    public void deleteNotification(String userId, String notificationId) {
        Notification notification = findNotification(userId, notificationId);
        notifications.delete(notification);
    }

    public void markAllRead(String userId) {
        var list = notifications.findByUserIdOrderByCreatedAtDesc(userId);
        if (list == null || list.isEmpty()) return;
        list.forEach(notification -> notification.setRead(true));
        notifications.saveAll(list);
    }

    private void applyChanges(Notification notification, NotificationRequest request) {
        notification.setTitle(request.title().trim());
        notification.setMessage(request.message().trim());
        notification.setType(request.type().trim());
        notification.setRead(request.read());
        notification.setTargetType(request.targetType());
        notification.setTargetId(request.targetId());
        notification.setTargetUrl(request.targetUrl());
    }

    private Notification findNotification(String userId, String notificationId) {
        return notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getTargetUrl());
    }
}
