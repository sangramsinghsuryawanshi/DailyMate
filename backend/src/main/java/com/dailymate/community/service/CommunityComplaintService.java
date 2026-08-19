package com.dailymate.community.service;

import com.dailymate.community.dto.request.CommunityComplaintRequest;
import com.dailymate.community.dto.response.CommunityComplaintResponse;
import com.dailymate.community.entity.CommunityComplaint;
import com.dailymate.community.repository.CommunityComplaintRepository;
import com.dailymate.core.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityComplaintService {

    private final CommunityComplaintRepository complaints;

    public CommunityComplaintService(CommunityComplaintRepository complaints) {
        this.complaints = complaints;
    }

    public List<CommunityComplaintResponse> getComplaints() {
        return complaints.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CommunityComplaintResponse createComplaint(CommunityComplaintRequest request) {
        CommunityComplaint complaint = new CommunityComplaint();
        applyChanges(complaint, request);
        return toResponse(complaints.save(complaint));
    }

    @Transactional
    public CommunityComplaintResponse updateComplaint(String complaintId, CommunityComplaintRequest request) {
        CommunityComplaint complaint = findComplaint(complaintId);
        applyChanges(complaint, request);
        return toResponse(complaints.save(complaint));
    }

    @Transactional
    public void deleteComplaint(String complaintId) {
        CommunityComplaint complaint = findComplaint(complaintId);
        complaints.delete(complaint);
    }

    private void applyChanges(CommunityComplaint complaint, CommunityComplaintRequest request) {
        complaint.setTitle(request.title().trim());
        complaint.setCategory(request.category().trim());
        complaint.setLocation(request.location().trim());
        complaint.setDescription(request.description().trim());
    }

    private CommunityComplaint findComplaint(String complaintId) {
        return complaints.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("Complaint not found"));
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
