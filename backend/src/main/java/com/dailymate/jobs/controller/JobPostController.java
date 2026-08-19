package com.dailymate.jobs.controller;

import com.dailymate.jobs.dto.request.JobPostRequest;
import com.dailymate.jobs.dto.response.JobPostResponse;
import com.dailymate.jobs.service.JobPostService;
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
@RequestMapping("/api/v1/jobs")
public class JobPostController {

    private final JobPostService jobPostService;

    public JobPostController(JobPostService jobPostService) {
        this.jobPostService = jobPostService;
    }

    @GetMapping("/posts")
    public List<JobPostResponse> getJobPosts() {
        return jobPostService.getJobPosts();
    }

    @PostMapping("/posts")
    public ResponseEntity<JobPostResponse> createJobPost(@Valid @RequestBody JobPostRequest request) {
        JobPostResponse response = jobPostService.createJobPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/posts/{id}")
    public JobPostResponse updateJobPost(@PathVariable String id, @Valid @RequestBody JobPostRequest request) {
        return jobPostService.updateJobPost(id, request);
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deleteJobPost(@PathVariable String id) {
        jobPostService.deleteJobPost(id);
        return ResponseEntity.noContent().build();
    }
}
