package com.dailymate.blood.entity;

public enum BloodUrgency {
    STANDARD,
    URGENT;

    public static BloodUrgency fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return STANDARD;
        }
        try {
            return BloodUrgency.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
