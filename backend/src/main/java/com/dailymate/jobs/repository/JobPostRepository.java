package com.dailymate.jobs.repository;

import com.dailymate.jobs.entity.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostRepository extends JpaRepository<JobPost, String> {
}
