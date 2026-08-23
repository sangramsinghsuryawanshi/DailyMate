package com.dailymate.emergency.repository;

import com.dailymate.emergency.entity.EmergencyContact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, String> {

    List<EmergencyContact> findAllByUserIdIsNullOrderByCreatedAtDesc();

    List<EmergencyContact> findAllByUserIdIsNullAndCategoryOrderByCreatedAtDesc(String category);

    List<EmergencyContact> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<EmergencyContact> findAllByUserIdAndCategoryOrderByCreatedAtDesc(String userId, String category);

    @Query("SELECT c FROM EmergencyContact c WHERE c.userId IS NULL AND " +
           "(:category IS NULL OR LOWER(c.category) = LOWER(:category))")
    Page<EmergencyContact> findPublicFiltered(@Param("category") String category, Pageable pageable);

    @Query("SELECT c FROM EmergencyContact c WHERE c.userId = :userId AND " +
           "(:category IS NULL OR LOWER(c.category) = LOWER(:category))")
    Page<EmergencyContact> findUserFiltered(@Param("userId") String userId, @Param("category") String category, Pageable pageable);

    Optional<EmergencyContact> findByIdAndUserId(String id, String userId);
}
