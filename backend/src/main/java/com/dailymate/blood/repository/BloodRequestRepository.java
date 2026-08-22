package com.dailymate.blood.repository;

import com.dailymate.blood.entity.BloodRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, String> {

    List<BloodRequest> findAllByOrderByCreatedAtDesc();

    List<BloodRequest> findAllByBloodGroupOrderByCreatedAtDesc(String bloodGroup);

    List<BloodRequest> findAllByStatusOrderByCreatedAtDesc(String status);

    List<BloodRequest> findAllByBloodGroupAndStatusOrderByCreatedAtDesc(String bloodGroup, String status);

    List<BloodRequest> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Optional<BloodRequest> findByIdAndUserId(String id, String userId);

    long countByStatus(String status);
}
