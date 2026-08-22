package com.dailymate.blood.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blood_requests")
public class BloodRequest {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "patient_name", nullable = false, length = 120)
    private String patientName;

    @Column(name = "blood_group", nullable = false, length = 10)
    private String bloodGroup;

    @Column(name = "units_needed", nullable = false)
    private int unitsNeeded;

    @Column(name = "hospital_location", nullable = false, length = 160)
    private String hospitalLocation;

    @Column(nullable = false, length = 20)
    private String urgency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "contact_name", nullable = false, length = 80)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 80)
    private String contactPhone;

    @Column(name = "additional_notes", length = 1000)
    private String additionalNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        id = id == null ? UUID.randomUUID().toString() : id;
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (urgency == null) urgency = BloodUrgency.STANDARD.name();
        if (status == null) status = BloodRequestStatus.OPEN.name();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public int getUnitsNeeded() { return unitsNeeded; }
    public void setUnitsNeeded(int unitsNeeded) { this.unitsNeeded = unitsNeeded; }
    public String getHospitalLocation() { return hospitalLocation; }
    public void setHospitalLocation(String hospitalLocation) { this.hospitalLocation = hospitalLocation; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
