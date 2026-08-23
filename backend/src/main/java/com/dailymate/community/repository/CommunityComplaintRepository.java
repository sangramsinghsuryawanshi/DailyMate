package com.dailymate.community.repository;

import com.dailymate.community.entity.CommunityComplaint;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityComplaintRepository extends JpaRepository<CommunityComplaint, String> {
    List<CommunityComplaint> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);

    @Query("SELECT c FROM CommunityComplaint c WHERE " +
           "(:category IS NULL OR LOWER(c.category) = LOWER(:category)) AND " +
           "(:status IS NULL OR UPPER(c.status) = UPPER(:status)) AND " +
           "(:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.location) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CommunityComplaint> findFiltered(
            @Param("category") String category,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
