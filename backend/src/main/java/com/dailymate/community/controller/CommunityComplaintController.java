package com.dailymate.community.controller;

import com.dailymate.community.dto.request.CommunityComplaintRequest;
import com.dailymate.community.dto.response.CommunityComplaintResponse;
import com.dailymate.community.service.CommunityComplaintService;
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
@RequestMapping("/api/v1/community-complaints")
public class CommunityComplaintController {

    private final CommunityComplaintService communityComplaintService;

    public CommunityComplaintController(CommunityComplaintService communityComplaintService) {
        this.communityComplaintService = communityComplaintService;
    }

    @GetMapping("/complaints")
    public List<CommunityComplaintResponse> getComplaints() {
        return communityComplaintService.getComplaints();
    }

    @PostMapping("/complaints")
    public ResponseEntity<CommunityComplaintResponse> createComplaint(@Valid @RequestBody CommunityComplaintRequest request) {
        CommunityComplaintResponse response = communityComplaintService.createComplaint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/complaints/{id}")
    public CommunityComplaintResponse updateComplaint(@PathVariable String id, @Valid @RequestBody CommunityComplaintRequest request) {
        return communityComplaintService.updateComplaint(id, request);
    }

    @DeleteMapping("/complaints/{id}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable String id) {
        communityComplaintService.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }
}
