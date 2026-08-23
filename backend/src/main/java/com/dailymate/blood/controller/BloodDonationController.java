package com.dailymate.blood.controller;

import com.dailymate.blood.dto.request.BloodRequestCreateRequest;
import com.dailymate.blood.dto.request.BloodRequestUpdateRequest;
import com.dailymate.blood.dto.request.DonationCenterRequest;
import com.dailymate.blood.dto.response.BloodRequestResponse;
import com.dailymate.blood.dto.response.DonationCenterResponse;
import com.dailymate.blood.service.BloodDonationService;
import com.dailymate.core.dto.response.PageResponse;
import com.dailymate.core.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blood")
public class BloodDonationController {

    private final BloodDonationService bloodDonationService;

    public BloodDonationController(BloodDonationService bloodDonationService) {
        this.bloodDonationService = bloodDonationService;
    }

    // --- Donation Centers Endpoints ---

    @GetMapping("/centers")
    public PageResponse<DonationCenterResponse> getCenters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bloodDonationService.getCenters(page, size);
    }

    @PostMapping("/centers")
    public ResponseEntity<DonationCenterResponse> createCenter(@Valid @RequestBody DonationCenterRequest request) {
        DonationCenterResponse response = bloodDonationService.createCenter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/centers/{id}")
    public DonationCenterResponse updateCenter(@PathVariable String id, @Valid @RequestBody DonationCenterRequest request) {
        return bloodDonationService.updateCenter(id, request);
    }

    @DeleteMapping("/centers/{id}")
    public ResponseEntity<Void> deleteCenter(@PathVariable String id) {
        bloodDonationService.deleteCenter(id);
        return ResponseEntity.noContent().build();
    }

    // --- Blood Requests Endpoints ---

    @GetMapping("/requests")
    public PageResponse<BloodRequestResponse> getRequests(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bloodDonationService.getRequests(bloodGroup, status, page, size);
    }

    @GetMapping("/my-requests")
    public PageResponse<BloodRequestResponse> getMyRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bloodDonationService.getMyRequests(principal.user().getId(), page, size);
    }

    @PostMapping("/requests")
    public ResponseEntity<BloodRequestResponse> createRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BloodRequestCreateRequest request) {
        BloodRequestResponse response = bloodDonationService.createRequest(principal.user().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/requests/{id}")
    public BloodRequestResponse updateRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody BloodRequestUpdateRequest request) {
        return bloodDonationService.updateRequest(principal.user().getId(), id, request);
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Void> deleteRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        bloodDonationService.deleteRequest(principal.user().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
