package com.dailymate.admin.service;

import com.dailymate.community.dto.response.CommunityComplaintResponse;
import com.dailymate.community.entity.CommunityComplaint;
import com.dailymate.community.repository.CommunityComplaintRepository;
import com.dailymate.core.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final CommunityComplaintRepository complaints;

    public AdminService(CommunityComplaintRepository complaints) {
        this.complaints = complaints;
    }

    public List<CommunityComplaintResponse> getComplaints() {
        return complaints.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommunityComplaintResponse updateComplaintStatus(String complaintId, String status) {
        CommunityComplaint complaint = complaints.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("Complaint not found"));
        complaint.setStatus(status.toUpperCase());
        return toResponse(complaints.save(complaint));
    }

    private CommunityComplaintResponse toResponse(CommunityComplaint complaint) {
        return new CommunityComplaintResponse(
                complaint.getId(),
                complaint.getTitle(),
                complaint.getCategory(),
                complaint.getLocation(),
                complaint.getDescription(),
                complaint.getStatus(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt());
    }
}
