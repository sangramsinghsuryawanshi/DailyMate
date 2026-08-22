package com.dailymate.emergency.repository;

import com.dailymate.emergency.entity.EmergencyContact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, String> {

    List<EmergencyContact> findAllByUserIdIsNullOrderByCreatedAtDesc();

    List<EmergencyContact> findAllByUserIdIsNullAndCategoryOrderByCreatedAtDesc(String category);

    List<EmergencyContact> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<EmergencyContact> findAllByUserIdAndCategoryOrderByCreatedAtDesc(String userId, String category);

    Optional<EmergencyContact> findByIdAndUserId(String id, String userId);
}
