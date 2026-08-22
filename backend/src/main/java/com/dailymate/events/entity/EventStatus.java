package com.dailymate.events.entity;

public enum EventStatus {
    PUBLISHED,
    CANCELLED,
    COMPLETED;

    public boolean canTransitionTo(EventStatus target) {
        if (target == null) return false;
        if (this == target) return true; // Idempotent
        return switch (this) {
            case PUBLISHED -> target == CANCELLED || target == COMPLETED;
            case CANCELLED -> target == PUBLISHED; // Can reopen
            case COMPLETED -> false; // Terminal
        };
    }

    public static EventStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return EventStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
