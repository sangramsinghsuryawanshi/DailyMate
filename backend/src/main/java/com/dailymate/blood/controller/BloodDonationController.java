package com.dailymate.blood.controller;

import com.dailymate.blood.dto.request.DonationCenterRequest;
import com.dailymate.blood.dto.response.DonationCenterResponse;
import com.dailymate.blood.service.BloodDonationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/blood")
public class BloodDonationController {

    private final BloodDonationService bloodDonationService;

    public BloodDonationController(BloodDonationService bloodDonationService) {
        this.bloodDonationService = bloodDonationService;
    }

    @GetMapping("/centers")
    public List<DonationCenterResponse> getCenters() {
        return bloodDonationService.getCenters();
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
}
