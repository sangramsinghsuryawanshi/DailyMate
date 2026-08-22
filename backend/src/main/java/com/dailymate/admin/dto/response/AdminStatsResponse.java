package com.dailymate.admin.dto.response;

public record AdminStatsResponse(
        long totalUsers,
        long activeUsers,
        long suspendedUsers,
        long totalComplaints,
        long openComplaints,
        long inReviewComplaints,
        long resolvedComplaints,
        long rejectedComplaints,
        long totalLostFound,
        long totalJobs,
        long openJobs,
        long closedJobs,
        long totalBloodRequests,
        long openBloodRequests,
        long fulfilledBloodRequests,
        long cancelledBloodRequests,
        long totalEvents,
        long publishedEvents,
        long cancelledEvents,
        long completedEvents) {
}
