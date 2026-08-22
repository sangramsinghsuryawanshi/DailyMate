package com.dailymate.assistant.security;

public interface AssistantRateLimiter {
    /**
     * Checks if the user is allowed to make a request.
     * Throws TooManyRequestsException if the rate limit is exceeded.
     *
     * @param userId Authenticated user ID
     */
    void checkLimit(String userId);

    /**
     * Resets limits (useful for testing and administrative resets).
     */
    void reset();
}
