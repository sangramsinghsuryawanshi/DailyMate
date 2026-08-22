package com.dailymate.admin.service;

import com.dailymate.admin.dto.response.AdminStatsResponse;
import com.dailymate.admin.dto.response.AdminUserResponse;
import com.dailymate.blood.dto.response.BloodRequestResponse;
import com.dailymate.blood.entity.BloodRequest;
import com.dailymate.blood.repository.BloodRequestRepository;
import com.dailymate.community.dto.response.CommunityComplaintResponse;
import com.dailymate.community.entity.CommunityComplaint;
import com.dailymate.community.entity.ComplaintStatus;
import com.dailymate.community.repository.CommunityComplaintRepository;
import com.dailymate.core.exception.BadRequestException;
import com.dailymate.core.exception.NotFoundException;
import com.dailymate.events.dto.response.LocalEventResponse;
import com.dailymate.events.entity.EventStatus;
import com.dailymate.events.entity.LocalEvent;
import com.dailymate.events.repository.LocalEventRepository;
import com.dailymate.jobs.dto.response.JobPostResponse;
import com.dailymate.jobs.entity.JobPost;
import com.dailymate.jobs.entity.JobStatus;
import com.dailymate.jobs.repository.JobPostRepository;
import com.dailymate.lostfound.dto.response.LostItemPostResponse;
import com.dailymate.lostfound.entity.LostItemPost;
import com.dailymate.lostfound.repository.LostItemPostRepository;
import com.dailymate.user.entity.User;
import com.dailymate.user.entity.UserRole;
import com.dailymate.user.entity.UserStatus;
import com.dailymate.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository users;
    private final CommunityComplaintRepository complaints;
    private final LostItemPostRepository lostItems;
    private final JobPostRepository jobs;
    private final BloodRequestRepository bloodRequests;
    private final LocalEventRepository events;

    public AdminService(
            UserRepository users,
            CommunityComplaintRepository complaints,
            LostItemPostRepository lostItems,
            JobPostRepository jobs,
            BloodRequestRepository bloodRequests,
            LocalEventRepository events) {
        this.users = users;
        this.complaints = complaints;
        this.lostItems = lostItems;
        this.jobs = jobs;
        this.bloodRequests = bloodRequests;
        this.events = events;
    }

    // 1. Live Database-Backed Statistics
    public AdminStatsResponse getStats() {
        long totalUsers = users.count();
        long activeUsers = users.countByStatus(UserStatus.ACTIVE);
        long suspendedUsers = users.countByStatus(UserStatus.SUSPENDED);

        long totalComplaints = complaints.count();
        long openComplaints = complaints.countByStatus("OPEN");
        long inReviewComplaints = complaints.countByStatus("IN_REVIEW");
        long resolvedComplaints = complaints.countByStatus("RESOLVED");
        long rejectedComplaints = complaints.countByStatus("REJECTED");

        long totalLostFound = lostItems.count();

        long totalJobs = jobs.count();
        long openJobs = jobs.countByStatus(JobStatus.OPEN);
        long closedJobs = jobs.countByStatus(JobStatus.CLOSED);

        long totalBloodRequests = bloodRequests.count();
        long openBloodRequests = bloodRequests.countByStatus("OPEN");
        long fulfilledBloodRequests = bloodRequests.countByStatus("FULFILLED");
        long cancelledBloodRequests = bloodRequests.countByStatus("CANCELLED");

        long totalEvents = events.count();
        long publishedEvents = events.countByStatus("PUBLISHED");
        long cancelledEvents = events.countByStatus("CANCELLED");
        long completedEvents = events.countByStatus("COMPLETED");

        return new AdminStatsResponse(
                totalUsers,
                activeUsers,
                suspendedUsers,
                totalComplaints,
                openComplaints,
                inReviewComplaints,
                resolvedComplaints,
                rejectedComplaints,
                totalLostFound,
                totalJobs,
                openJobs,
                closedJobs,
                totalBloodRequests,
                openBloodRequests,
                fulfilledBloodRequests,
                cancelledBloodRequests,
                totalEvents,
                publishedEvents,
                cancelledEvents,
                completedEvents);
    }

    // 2. Complaints Moderation
    public List<CommunityComplaintResponse> getComplaints() {
        return complaints.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toComplaintResponse)
                .toList();
    }

    @Transactional
    public CommunityComplaintResponse updateComplaintStatus(String complaintId, String status) {
        CommunityComplaint complaint = complaints.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("Complaint not found"));

        ComplaintStatus currentStatus = ComplaintStatus.fromString(complaint.getStatus());
        if (currentStatus == null) {
            currentStatus = ComplaintStatus.OPEN;
        }

        ComplaintStatus targetStatus = ComplaintStatus.fromString(status);
        if (targetStatus == null) {
            throw new BadRequestException("Invalid complaint status: " + status);
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + targetStatus);
        }

        complaint.setStatus(targetStatus.name());
        return toComplaintResponse(complaints.save(complaint));
    }

    // 3. Lost & Found Moderation
    public List<LostItemPostResponse> getLostFound() {
        return lostItems.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toLostItemResponse)
                .toList();
    }

    @Transactional
    public void deleteLostFound(String id) {
        LostItemPost item = lostItems.findById(id)
                .orElseThrow(() -> new NotFoundException("Lost item post not found"));
        lostItems.delete(item);
    }

    // 4. Jobs Moderation
    public List<JobPostResponse> getJobs() {
        return jobs.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toJobResponse)
                .toList();
    }

    @Transactional
    public JobPostResponse updateJobStatus(String id, String status) {
        JobPost post = jobs.findById(id)
                .orElseThrow(() -> new NotFoundException("Job post not found"));

        try {
            JobStatus targetStatus = JobStatus.valueOf(status.trim().toUpperCase());
            post.setStatus(targetStatus);
            return toJobResponse(jobs.save(post));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid job status: " + status);
        }
    }

    @Transactional
    public void deleteJob(String id) {
        JobPost post = jobs.findById(id)
                .orElseThrow(() -> new NotFoundException("Job post not found"));
        jobs.delete(post);
    }

    // 5. Blood Requests Moderation
    public List<BloodRequestResponse> getBloodRequests() {
        return bloodRequests.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toBloodResponse)
                .toList();
    }

    @Transactional
    public BloodRequestResponse updateBloodRequestStatus(String id, String status) {
        BloodRequest request = bloodRequests.findById(id)
                .orElseThrow(() -> new NotFoundException("Blood request not found"));

        String normalized = status.trim().toUpperCase();
        if (!"OPEN".equals(normalized) && !"FULFILLED".equals(normalized) && !"CANCELLED".equals(normalized)) {
            throw new BadRequestException("Invalid blood request status: " + status);
        }

        request.setStatus(normalized);
        return toBloodResponse(bloodRequests.save(request));
    }

    @Transactional
    public void deleteBloodRequest(String id) {
        BloodRequest request = bloodRequests.findById(id)
                .orElseThrow(() -> new NotFoundException("Blood request not found"));
        bloodRequests.delete(request);
    }

    // 6. Local Events Moderation
    public List<LocalEventResponse> getEvents() {
        return events.findAllByOrderByEventDateAsc().stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Transactional
    public LocalEventResponse updateEventStatus(String id, String status) {
        LocalEvent event = events.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        try {
            EventStatus targetStatus = EventStatus.valueOf(status.trim().toUpperCase());
            event.setStatus(targetStatus.name());
            return toEventResponse(events.save(event));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid event status: " + status);
        }
    }

    @Transactional
    public void deleteEvent(String id) {
        LocalEvent event = events.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        events.delete(event);
    }

    // 7. User Account Management
    public List<AdminUserResponse> getUsers() {
        return users.findAll().stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUserStatus(String currentAdminUserId, String targetUserId, String status) {
        if (currentAdminUserId != null && currentAdminUserId.equals(targetUserId)) {
            throw new BadRequestException("Administrator cannot modify their own account status");
        }

        User targetUser = users.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (targetUser.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Cannot modify status of administrator accounts");
        }

        try {
            UserStatus targetStatus = UserStatus.valueOf(status.trim().toUpperCase());
            targetUser.setStatus(targetStatus);
            return toUserResponse(users.save(targetUser));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid user status: " + status);
        }
    }

    // Mappers
    private CommunityComplaintResponse toComplaintResponse(CommunityComplaint complaint) {
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

    private LostItemPostResponse toLostItemResponse(LostItemPost item) {
        return new LostItemPostResponse(
                item.getId(),
                item.getUserId(),
                item.getTitle(),
                item.getItemType(),
                item.getLocation(),
                item.getDescription(),
                item.getContactName(),
                item.getContactPhone(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    private JobPostResponse toJobResponse(JobPost post) {
        return new JobPostResponse(
                post.getId(),
                post.getUserId(),
                post.getTitle(),
                post.getCategory(),
                post.getLocation(),
                post.getType(),
                post.getSalary(),
                post.getCompanyName(),
                post.getContactPhone(),
                post.getContactEmail(),
                post.getStatus() != null ? post.getStatus().name() : "OPEN",
                post.getDescription(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    private BloodRequestResponse toBloodResponse(BloodRequest req) {
        return new BloodRequestResponse(
                req.getId(),
                req.getUserId(),
                req.getPatientName(),
                req.getBloodGroup(),
                req.getUnitsNeeded(),
                req.getHospitalLocation(),
                req.getUrgency(),
                req.getStatus(),
                req.getContactName(),
                req.getContactPhone(),
                req.getAdditionalNotes(),
                req.getCreatedAt(),
                req.getUpdatedAt());
    }

    private LocalEventResponse toEventResponse(LocalEvent event) {
        return new LocalEventResponse(
                event.getId(),
                event.getUserId(),
                event.getTitle(),
                event.getCategory(),
                event.getLocation(),
                event.getEventDate(),
                event.getDescription(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    private AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
