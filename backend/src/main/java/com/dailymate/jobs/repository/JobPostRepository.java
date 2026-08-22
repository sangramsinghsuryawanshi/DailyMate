package com.dailymate.jobs.repository;

import com.dailymate.jobs.entity.JobPost;
import com.dailymate.jobs.entity.JobStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostRepository extends JpaRepository<JobPost, String> {

    List<JobPost> findAllByOrderByCreatedAtDesc();

    List<JobPost> findAllByStatusOrderByCreatedAtDesc(JobStatus status);

    List<JobPost> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Optional<JobPost> findByIdAndUserId(String id, String userId);

    List<JobPost> findAllByCategoryAndStatusOrderByCreatedAtDesc(String category, JobStatus status);

    long countByStatus(JobStatus status);
}
