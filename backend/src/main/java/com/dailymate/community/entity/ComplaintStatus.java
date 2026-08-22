package com.dailymate.community.entity;

public enum ComplaintStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED,
    REJECTED;

    public boolean canTransitionTo(ComplaintStatus target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true; // Idempotent
        }
        return switch (this) {
            case OPEN -> target == IN_REVIEW || target == REJECTED;
            case IN_REVIEW -> target == RESOLVED || target == REJECTED || target == OPEN;
            case RESOLVED -> target == OPEN;
            case REJECTED -> target == OPEN;
        };
    }

    public static ComplaintStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return ComplaintStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
