package com.dailymate.admin.controller;

import com.dailymate.admin.dto.request.AdminComplaintStatusRequest;
import com.dailymate.admin.dto.request.AdminStatusUpdateRequest;
import com.dailymate.admin.dto.response.AdminStatsResponse;
import com.dailymate.admin.dto.response.AdminUserResponse;
import com.dailymate.admin.service.AdminService;
import com.dailymate.blood.dto.response.BloodRequestResponse;
import com.dailymate.community.dto.response.CommunityComplaintResponse;
import com.dailymate.core.security.UserPrincipal;
import com.dailymate.events.dto.response.LocalEventResponse;
import com.dailymate.jobs.dto.response.JobPostResponse;
import com.dailymate.lostfound.dto.response.LostItemPostResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // 1. Overview Statistics
    @GetMapping("/stats")
    public AdminStatsResponse getStats() {
        return adminService.getStats();
    }

    // 2. Complaints Moderation
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

    // 3. Lost & Found Moderation
    @GetMapping("/lost-found")
    public List<LostItemPostResponse> getLostFound() {
        return adminService.getLostFound();
    }

    @DeleteMapping("/lost-found/{id}")
    public ResponseEntity<Void> deleteLostFound(@PathVariable String id) {
        adminService.deleteLostFound(id);
        return ResponseEntity.noContent().build();
    }

    // 4. Jobs Moderation
    @GetMapping("/jobs")
    public List<JobPostResponse> getJobs() {
        return adminService.getJobs();
    }

    @PatchMapping("/jobs/{id}/status")
    public ResponseEntity<JobPostResponse> updateJobStatus(
            @PathVariable String id,
            @Valid @RequestBody AdminStatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateJobStatus(id, request.status()));
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable String id) {
        adminService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    // 5. Blood Requests Moderation
    @GetMapping("/blood-requests")
    public List<BloodRequestResponse> getBloodRequests() {
        return adminService.getBloodRequests();
    }

    @PatchMapping("/blood-requests/{id}/status")
    public ResponseEntity<BloodRequestResponse> updateBloodRequestStatus(
            @PathVariable String id,
            @Valid @RequestBody AdminStatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateBloodRequestStatus(id, request.status()));
    }

    @DeleteMapping("/blood-requests/{id}")
    public ResponseEntity<Void> deleteBloodRequest(@PathVariable String id) {
        adminService.deleteBloodRequest(id);
        return ResponseEntity.noContent().build();
    }

    // 6. Local Events Moderation
    @GetMapping("/events")
    public List<LocalEventResponse> getEvents() {
        return adminService.getEvents();
    }

    @PatchMapping("/events/{id}/status")
    public ResponseEntity<LocalEventResponse> updateEventStatus(
            @PathVariable String id,
            @Valid @RequestBody AdminStatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateEventStatus(id, request.status()));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        adminService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    // 7. User Account Management
    @GetMapping("/users")
    public List<AdminUserResponse> getUsers() {
        return adminService.getUsers();
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody AdminStatusUpdateRequest request) {
        String adminUserId = principal != null ? principal.user().getId() : null;
        return ResponseEntity.ok(adminService.updateUserStatus(adminUserId, id, request.status()));
    }
}
