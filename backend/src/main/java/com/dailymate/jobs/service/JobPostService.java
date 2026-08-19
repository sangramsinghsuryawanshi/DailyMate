package com.dailymate.jobs.service;

import com.dailymate.core.exception.NotFoundException;
import com.dailymate.jobs.dto.request.JobPostRequest;
import com.dailymate.jobs.dto.response.JobPostResponse;
import com.dailymate.jobs.entity.JobPost;
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

    public List<JobPostResponse> getJobPosts() {
        return jobPosts.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public JobPostResponse createJobPost(JobPostRequest request) {
        JobPost post = new JobPost();
        applyChanges(post, request);
        return toResponse(jobPosts.save(post));
    }

    @Transactional
    public JobPostResponse updateJobPost(String jobId, JobPostRequest request) {
        JobPost post = findJobPost(jobId);
        applyChanges(post, request);
        return toResponse(jobPosts.save(post));
    }

    @Transactional
    public void deleteJobPost(String jobId) {
        JobPost post = findJobPost(jobId);
        jobPosts.delete(post);
    }

    private void applyChanges(JobPost post, JobPostRequest request) {
        post.setTitle(request.title().trim());
        post.setCategory(request.category().trim());
        post.setLocation(request.location().trim());
        post.setType(request.type().trim());
        post.setDescription(request.description().trim());
    }

    private JobPost findJobPost(String jobId) {
        return jobPosts.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job post not found"));
    }

    private JobPostResponse toResponse(JobPost post) {
        return new JobPostResponse(
                post.getId(),
                post.getTitle(),
                post.getCategory(),
                post.getLocation(),
                post.getType(),
                post.getDescription(),
                post.getCreatedAt());
    }
}
