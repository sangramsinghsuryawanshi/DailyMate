package com.dailymate.assistant.security;

import com.dailymate.core.exception.TooManyRequestsException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory sliding-window rate limiter per authenticated user.
 * Note: For multi-replica production deployments, this can be swapped with a Redis-backed implementation.
 */
@Component
public class InMemoryAssistantRateLimiter implements AssistantRateLimiter {

    private final Map<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;

    public InMemoryAssistantRateLimiter(
            @Value("${dailymate.assistant.rate-limit.max-requests:20}") int maxRequests,
            @Value("${dailymate.assistant.rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    public void checkLimit(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - windowMillis;

        requestWindows.compute(userId, (key, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
            }

            // Evict expired timestamps outside current window
            while (!deque.isEmpty() && deque.peekFirst() <= windowStart) {
                deque.pollFirst();
            }

            if (deque.size() >= maxRequests) {
                throw new TooManyRequestsException("Rate limit exceeded for assistant requests. Please wait before sending another prompt.");
            }

            deque.addLast(now);
            return deque;
        });
    }

    @Override
    public void reset() {
        requestWindows.clear();
    }
}
