package com.dailymate.assistant.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Domain-specific intermediate data models for parsed segments before mutation parameter mapping.
 * Invariant: Retains sourceText tracking where practical.
 */
public class ParsedDomainModels {

    public record ParsedExpense(
            String description,
            BigDecimal amount,
            String category,
            LocalDate spentOn,
            String notes,
            String sourceText) {
        public ParsedExpense(String description, BigDecimal amount, String category, LocalDate spentOn, String notes) {
            this(description, amount, category, spentOn, notes, null);
        }
    }

    public record ParsedMedicineReminder(
            String name,
            String dosage,
            String frequency,
            LocalTime remindAt,
            String notes,
            String sourceText) {
        public ParsedMedicineReminder(String name, String dosage, String frequency, LocalTime remindAt, String notes) {
            this(name, dosage, frequency, remindAt, notes, null);
        }
    }

    public record ParsedEmergencyContact(
            String name,
            String relationship,
            String phone,
            String category,
            String notes,
            String sourceText) {
        public ParsedEmergencyContact(String name, String relationship, String phone, String category, String notes) {
            this(name, relationship, phone, category, notes, null);
        }
    }

    public record ParsedComplaint(
            String title,
            String description,
            String category,
            String location,
            String sourceText) {
        public ParsedComplaint(String title, String description, String category, String location) {
            this(title, description, category, location, null);
        }
    }

    public record ParsedLostItem(
            String title,
            String description,
            String location,
            String type, // LOST or FOUND
            String sourceText) {
        public ParsedLostItem(String title, String description, String location, String type) {
            this(title, description, location, type, null);
        }
    }

    public record ParsedEvent(
            String title,
            String description,
            LocalDate eventDate,
            LocalTime eventTime,
            String location,
            String category,
            String sourceText) {
        public ParsedEvent(String title, String description, LocalDate eventDate, LocalTime eventTime, String location, String category) {
            this(title, description, eventDate, eventTime, location, category, null);
        }
    }

    public record ParsedGroceryItem(
            String name,
            String category,
            String store,
            BigDecimal price,
            String unit,
            String location,
            String sourceText) {
    }

    public record ParsedJobPost(
            String title,
            String category,
            String location,
            String type,
            String salary,
            String companyName,
            String contactPhone,
            String contactEmail,
            String description,
            String sourceText) {
    }

    public record ParsedBloodRequest(
            String patientName,
            String bloodGroup,
            String hospital,
            String contactPhone,
            String urgency,
            String notes,
            String sourceText) {
    }

    public record ParsedNotification(
            String title,
            String message,
            String type,
            String sourceText) {
    }

    public record ParsedProvider(
            String businessName,
            String category,
            String phone,
            String email,
            String address,
            String description,
            String sourceText) {
    }
}
