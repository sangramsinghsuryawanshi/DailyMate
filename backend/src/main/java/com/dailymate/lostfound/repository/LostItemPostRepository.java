package com.dailymate.lostfound.repository;

import com.dailymate.lostfound.entity.LostItemPost;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LostItemPostRepository extends JpaRepository<LostItemPost, String> {
    List<LostItemPost> findByUserIdOrderByCreatedAtDesc(String userId);
    List<LostItemPost> findAllByOrderByCreatedAtDesc();
    Optional<LostItemPost> findByIdAndUserId(String id, String userId);

    @Query("SELECT p FROM LostItemPost p WHERE " +
           "(:type IS NULL OR LOWER(p.itemType) = LOWER(:type)) AND " +
           "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.location) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<LostItemPost> findFiltered(
            @Param("type") String type,
            @Param("search") String search,
            Pageable pageable);

    Page<LostItemPost> findByUserId(String userId, Pageable pageable);
}
