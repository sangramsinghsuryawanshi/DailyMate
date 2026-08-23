package com.dailymate.jobs.repository;

import com.dailymate.jobs.entity.JobPost;
import com.dailymate.jobs.entity.JobStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobPostRepository extends JpaRepository<JobPost, String> {

    List<JobPost> findAllByOrderByCreatedAtDesc();

    List<JobPost> findAllByStatusOrderByCreatedAtDesc(JobStatus status);

    List<JobPost> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<JobPost> findAllByCategoryAndStatusOrderByCreatedAtDesc(String category, JobStatus status);

    @Query("SELECT j FROM JobPost j WHERE " +
           "(:status IS NULL OR j.status = :status) AND " +
           "(:category IS NULL OR LOWER(j.category) = LOWER(:category)) AND " +
           "(:type IS NULL OR LOWER(j.type) = LOWER(:type)) AND " +
           "(:search IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(j.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           " OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<JobPost> findFiltered(
            @Param("status") JobStatus status,
            @Param("category") String category,
            @Param("type") String type,
            @Param("search") String search,
            Pageable pageable);

    Page<JobPost> findByUserId(String userId, Pageable pageable);

    Optional<JobPost> findByIdAndUserId(String id, String userId);

    long countByStatus(JobStatus status);
}
