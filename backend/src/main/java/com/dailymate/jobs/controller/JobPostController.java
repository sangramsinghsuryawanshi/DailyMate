package com.dailymate.jobs.controller;

import com.dailymate.core.dto.response.PageResponse;
import com.dailymate.core.security.UserPrincipal;
import com.dailymate.jobs.dto.request.JobPostRequest;
import com.dailymate.jobs.dto.response.JobPostResponse;
import com.dailymate.jobs.service.JobPostService;
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
@RequestMapping("/api/v1/jobs")
public class JobPostController {

    private final JobPostService jobPostService;

    public JobPostController(JobPostService jobPostService) {
        this.jobPostService = jobPostService;
    }

    @GetMapping("/posts")
    public PageResponse<JobPostResponse> getJobPosts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobPostService.getJobPosts(search, category, type, status, page, size);
    }

    @GetMapping("/my-posts")
    public PageResponse<JobPostResponse> getMyJobPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobPostService.getMyJobPosts(principal.user().getId(), page, size);
    }

    @PostMapping("/posts")
    public ResponseEntity<JobPostResponse> createJobPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody JobPostRequest request) {
        JobPostResponse response = jobPostService.createJobPost(principal.user().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/posts/{id}")
    public JobPostResponse updateJobPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody JobPostRequest request) {
        return jobPostService.updateJobPost(principal.user().getId(), id, request);
    }

    @PatchMapping("/posts/{id}/status")
    public JobPostResponse changeStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @RequestParam String status) {
        return jobPostService.changeStatus(principal.user().getId(), id, status);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deleteJobPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        jobPostService.deleteJobPost(principal.user().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
