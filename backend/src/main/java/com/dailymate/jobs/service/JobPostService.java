package com.dailymate.jobs.service;

import com.dailymate.core.exception.NotFoundException;
import com.dailymate.jobs.dto.request.JobPostRequest;
import com.dailymate.jobs.dto.response.JobPostResponse;
import com.dailymate.jobs.entity.JobPost;
import com.dailymate.jobs.entity.JobStatus;
import com.dailymate.jobs.repository.JobPostRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobPostService {

    private final JobPostRepository jobPosts;

    public JobPostService(JobPostRepository jobPosts) {
        this.jobPosts = jobPosts;
    }

    public List<JobPostResponse> getJobPosts(String search, String category, String type, String status) {
        List<JobPost> list = jobPosts.findAllByOrderByCreatedAtDesc();

        // Default public feed to OPEN jobs unless a specific filter or ALL is requested
        final String effectiveStatus;
        if (status == null || status.isBlank()) {
            effectiveStatus = "OPEN";
        } else {
            effectiveStatus = status.trim().toUpperCase();
        }

        return list.stream()
                .filter(post -> {
                    if ("ALL".equalsIgnoreCase(effectiveStatus)) {
                        return true;
                    }
                    return post.getStatus() != null && post.getStatus().name().equalsIgnoreCase(effectiveStatus);
                })
                .filter(post -> {
                    if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
                        return true;
                    }
                    return post.getCategory() != null && post.getCategory().equalsIgnoreCase(category.trim());
                })
                .filter(post -> {
                    if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) {
                        return true;
                    }
                    return post.getType() != null && post.getType().equalsIgnoreCase(type.trim());
                })
                .filter(post -> {
                    if (search == null || search.isBlank()) {
                        return true;
                    }
                    String q = search.trim().toLowerCase();
                    boolean matchTitle = post.getTitle() != null && post.getTitle().toLowerCase().contains(q);
                    boolean matchDesc = post.getDescription() != null && post.getDescription().toLowerCase().contains(q);
                    boolean matchCompany = post.getCompanyName() != null && post.getCompanyName().toLowerCase().contains(q);
                    return matchTitle || matchDesc || matchCompany;
                })
                .map(this::toResponse)
                .toList();
    }

    public List<JobPostResponse> getMyJobPosts(String userId) {
        return jobPosts.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public JobPostResponse createJobPost(String userId, JobPostRequest request) {
        JobPost post = new JobPost();
        post.setUserId(userId);
        post.setStatus(JobStatus.OPEN); // Server defaults to OPEN on creation
        applyChanges(post, request);
        return toResponse(jobPosts.save(post));
    }

    @Transactional
    public JobPostResponse updateJobPost(String userId, String jobId, JobPostRequest request) {
        JobPost post = jobPosts.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new NotFoundException("Job post not found"));

        applyChanges(post, request);

        // Controlled lifecycle transition if status supplied
        if (request.status() != null && !request.status().isBlank()) {
            try {
                JobStatus targetStatus = JobStatus.valueOf(request.status().trim().toUpperCase());
                post.setStatus(targetStatus);
            } catch (IllegalArgumentException ex) {
                // Invalid status ignored, retains current status
            }
        }

        return toResponse(jobPosts.save(post));
    }

    @Transactional
    public void deleteJobPost(String userId, String jobId) {
        JobPost post = jobPosts.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new NotFoundException("Job post not found"));
        jobPosts.delete(post);
    }

    private void applyChanges(JobPost post, JobPostRequest request) {
        post.setTitle(request.title().trim());
        post.setCategory(request.category().trim());
        post.setLocation(request.location().trim());
        post.setType(request.type().trim());
        post.setSalary(request.salary());
        post.setCompanyName(request.companyName() != null ? request.companyName().trim() : null);
        post.setContactPhone(request.contactPhone() != null ? request.contactPhone().trim() : null);
        post.setContactEmail(request.contactEmail() != null ? request.contactEmail().trim() : null);
        post.setDescription(request.description().trim());
    }

    private JobPostResponse toResponse(JobPost post) {
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
}
