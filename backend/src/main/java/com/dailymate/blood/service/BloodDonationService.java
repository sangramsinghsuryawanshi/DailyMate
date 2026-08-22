package com.dailymate.blood.service;

import com.dailymate.blood.dto.request.BloodRequestCreateRequest;
import com.dailymate.blood.dto.request.BloodRequestUpdateRequest;
import com.dailymate.blood.dto.request.DonationCenterRequest;
import com.dailymate.blood.dto.response.BloodRequestResponse;
import com.dailymate.blood.dto.response.DonationCenterResponse;
import com.dailymate.blood.entity.BloodGroup;
import com.dailymate.blood.entity.BloodRequest;
import com.dailymate.blood.entity.BloodRequestStatus;
import com.dailymate.blood.entity.BloodUrgency;
import com.dailymate.blood.entity.DonationCenter;
import com.dailymate.blood.repository.BloodRequestRepository;
import com.dailymate.blood.repository.DonationCenterRepository;
import com.dailymate.core.exception.BadRequestException;
import com.dailymate.core.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BloodDonationService {

    private final DonationCenterRepository centers;
    private final BloodRequestRepository bloodRequests;

    public BloodDonationService(DonationCenterRepository centers, BloodRequestRepository bloodRequests) {
        this.centers = centers;
        this.bloodRequests = bloodRequests;
    }

    // --- Donation Centers ---

    public List<DonationCenterResponse> getCenters() {
        return centers.findAll().stream()
                .map(this::toCenterResponse)
                .toList();
    }

    public DonationCenterResponse createCenter(DonationCenterRequest request) {
        DonationCenter center = new DonationCenter();
        applyCenterChanges(center, request);
        return toCenterResponse(centers.save(center));
    }

    @Transactional
    public DonationCenterResponse updateCenter(String centerId, DonationCenterRequest request) {
        DonationCenter center = findCenter(centerId);
        applyCenterChanges(center, request);
        return toCenterResponse(centers.save(center));
    }

    @Transactional
    public void deleteCenter(String centerId) {
        DonationCenter center = findCenter(centerId);
        centers.delete(center);
    }

    // --- Blood Requests ---

    public List<BloodRequestResponse> getRequests(String bloodGroup, String status) {
        String normalizedGroup = (bloodGroup != null && !bloodGroup.trim().isEmpty()) ? bloodGroup.trim().toUpperCase() : null;
        String normalizedStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;

        List<BloodRequest> result;
        if (normalizedGroup != null && normalizedStatus != null) {
            result = bloodRequests.findAllByBloodGroupAndStatusOrderByCreatedAtDesc(normalizedGroup, normalizedStatus);
        } else if (normalizedGroup != null) {
            result = bloodRequests.findAllByBloodGroupOrderByCreatedAtDesc(normalizedGroup);
        } else if (normalizedStatus != null) {
            result = bloodRequests.findAllByStatusOrderByCreatedAtDesc(normalizedStatus);
        } else {
            result = bloodRequests.findAllByOrderByCreatedAtDesc();
        }

        return result.stream().map(this::toRequestResponse).toList();
    }

    public List<BloodRequestResponse> getMyRequests(String userId) {
        return bloodRequests.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toRequestResponse)
                .toList();
    }

    @Transactional
    public BloodRequestResponse createRequest(String userId, BloodRequestCreateRequest request) {
        BloodGroup group = BloodGroup.fromString(request.bloodGroup());
        if (group == null) {
            throw new BadRequestException("Invalid blood group: " + request.bloodGroup());
        }

        BloodUrgency urgency = BloodUrgency.fromString(request.urgency());

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setUserId(userId);
        bloodRequest.setPatientName(request.patientName().trim());
        bloodRequest.setBloodGroup(group.getLabel());
        bloodRequest.setUnitsNeeded(request.unitsNeeded());
        bloodRequest.setHospitalLocation(request.hospitalLocation().trim());
        bloodRequest.setUrgency(urgency.name());
        bloodRequest.setStatus(BloodRequestStatus.OPEN.name());
        bloodRequest.setContactName(request.contactName().trim());
        bloodRequest.setContactPhone(request.contactPhone().trim());
        bloodRequest.setAdditionalNotes(request.additionalNotes() != null ? request.additionalNotes().trim() : null);

        return toRequestResponse(bloodRequests.save(bloodRequest));
    }

    @Transactional
    public BloodRequestResponse updateRequest(String userId, String requestId, BloodRequestUpdateRequest request) {
        BloodRequest bloodRequest = bloodRequests.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Blood request not found"));

        BloodGroup group = BloodGroup.fromString(request.bloodGroup());
        if (group == null) {
            throw new BadRequestException("Invalid blood group: " + request.bloodGroup());
        }

        BloodRequestStatus currentStatus = BloodRequestStatus.fromString(bloodRequest.getStatus());
        BloodRequestStatus targetStatus = BloodRequestStatus.fromString(request.status());

        if (targetStatus != null && currentStatus != null && !currentStatus.canTransitionTo(targetStatus)) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + targetStatus);
        }

        BloodUrgency urgency = BloodUrgency.fromString(request.urgency());

        bloodRequest.setPatientName(request.patientName().trim());
        bloodRequest.setBloodGroup(group.getLabel());
        bloodRequest.setUnitsNeeded(request.unitsNeeded());
        bloodRequest.setHospitalLocation(request.hospitalLocation().trim());
        bloodRequest.setUrgency(urgency.name());
        if (targetStatus != null) {
            bloodRequest.setStatus(targetStatus.name());
        }
        bloodRequest.setContactName(request.contactName().trim());
        bloodRequest.setContactPhone(request.contactPhone().trim());
        bloodRequest.setAdditionalNotes(request.additionalNotes() != null ? request.additionalNotes().trim() : null);

        return toRequestResponse(bloodRequests.save(bloodRequest));
    }

    @Transactional
    public void deleteRequest(String userId, String requestId) {
        BloodRequest bloodRequest = bloodRequests.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Blood request not found"));
        bloodRequests.delete(bloodRequest);
    }

    // --- Helpers ---

    private void applyCenterChanges(DonationCenter center, DonationCenterRequest request) {
        center.setName(request.name().trim());
        center.setLocation(request.location().trim());
        center.setContact(request.contact().trim());
        center.setDescription(request.description().trim());
    }

    private DonationCenter findCenter(String centerId) {
        return centers.findById(centerId)
                .orElseThrow(() -> new NotFoundException("Donation center not found"));
    }

    private DonationCenterResponse toCenterResponse(DonationCenter center) {
        return new DonationCenterResponse(
                center.getId(),
                center.getName(),
                center.getLocation(),
                center.getContact(),
                center.getDescription(),
                center.getCreatedAt());
    }

    private BloodRequestResponse toRequestResponse(BloodRequest request) {
        return new BloodRequestResponse(
                request.getId(),
                request.getUserId(),
                request.getPatientName(),
                request.getBloodGroup(),
                request.getUnitsNeeded(),
                request.getHospitalLocation(),
                request.getUrgency(),
                request.getStatus(),
                request.getContactName(),
                request.getContactPhone(),
                request.getAdditionalNotes(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
