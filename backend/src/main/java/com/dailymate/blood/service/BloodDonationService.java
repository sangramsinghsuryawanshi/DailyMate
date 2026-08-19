package com.dailymate.blood.service;

import com.dailymate.blood.dto.request.DonationCenterRequest;
import com.dailymate.blood.dto.response.DonationCenterResponse;
import com.dailymate.blood.entity.DonationCenter;
import com.dailymate.blood.repository.DonationCenterRepository;
import com.dailymate.core.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BloodDonationService {

    private final DonationCenterRepository centers;

    public BloodDonationService(DonationCenterRepository centers) {
        this.centers = centers;
    }

    public List<DonationCenterResponse> getCenters() {
        return centers.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public DonationCenterResponse createCenter(DonationCenterRequest request) {
        DonationCenter center = new DonationCenter();
        applyChanges(center, request);
        return toResponse(centers.save(center));
    }

    @Transactional
    public DonationCenterResponse updateCenter(String centerId, DonationCenterRequest request) {
        DonationCenter center = findCenter(centerId);
        applyChanges(center, request);
        return toResponse(centers.save(center));
    }

    @Transactional
    public void deleteCenter(String centerId) {
        DonationCenter center = findCenter(centerId);
        centers.delete(center);
    }

    private void applyChanges(DonationCenter center, DonationCenterRequest request) {
        center.setName(request.name().trim());
        center.setLocation(request.location().trim());
        center.setContact(request.contact().trim());
        center.setDescription(request.description().trim());
    }

    private DonationCenter findCenter(String centerId) {
        return centers.findById(centerId)
                .orElseThrow(() -> new NotFoundException("Donation center not found"));
    }

    private DonationCenterResponse toResponse(DonationCenter center) {
        return new DonationCenterResponse(
                center.getId(),
                center.getName(),
                center.getLocation(),
                center.getContact(),
                center.getDescription(),
                center.getCreatedAt());
    }
}
