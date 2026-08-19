package com.dailymate.admin.controller;

import com.dailymate.admin.dto.request.AdminComplaintStatusRequest;
import com.dailymate.admin.service.AdminService;
import com.dailymate.community.dto.response.CommunityComplaintResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/complaints")
    public List<CommunityComplaintResponse> getComplaints() {
        return adminService.getComplaints();
    }

    @PatchMapping("/complaints/{id}/status")
    public ResponseEntity<CommunityComplaintResponse> updateComplaintStatus(
            @PathVariable String id,
            @Valid @RequestBody AdminComplaintStatusRequest request) {
        return ResponseEntity.ok(adminService.updateComplaintStatus(id, request.status()));
    }
}
