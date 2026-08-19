package com.dailymate.notification.dto.response;

import java.util.List;

public record NotificationPageResponse(List<NotificationResponse> content, long totalElements, int page, int size) {
}
