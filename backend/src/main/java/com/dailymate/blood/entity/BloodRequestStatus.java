package com.dailymate.blood.entity;

public enum BloodRequestStatus {
    OPEN,
    FULFILLED,
    CANCELLED;

    public boolean canTransitionTo(BloodRequestStatus target) {
        if (target == null) return false;
        if (this == target) return true; // Idempotent
        return switch (this) {
            case OPEN -> target == FULFILLED || target == CANCELLED;
            case FULFILLED, CANCELLED -> false; // Terminal states
        };
    }

    public static BloodRequestStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return BloodRequestStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
