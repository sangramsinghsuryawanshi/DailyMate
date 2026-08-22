package com.dailymate.blood.entity;

public enum BloodGroup {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static BloodGroup fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "");
        for (BloodGroup bg : values()) {
            if (bg.name().equalsIgnoreCase(normalized) || bg.label.equalsIgnoreCase(normalized)) {
                return bg;
            }
        }
        return null;
    }
}
