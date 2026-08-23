package com.dailymate.blood.repository;

import com.dailymate.blood.entity.BloodRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, String> {

    List<BloodRequest> findAllByOrderByCreatedAtDesc();

    List<BloodRequest> findAllByBloodGroupOrderByCreatedAtDesc(String bloodGroup);

    List<BloodRequest> findAllByStatusOrderByCreatedAtDesc(String status);

    List<BloodRequest> findAllByBloodGroupAndStatusOrderByCreatedAtDesc(String bloodGroup, String status);

    List<BloodRequest> findAllByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT r FROM BloodRequest r WHERE " +
           "(:bloodGroup IS NULL OR UPPER(r.bloodGroup) = UPPER(:bloodGroup)) AND " +
           "(:status IS NULL OR UPPER(r.status) = UPPER(:status))")
    Page<BloodRequest> findFiltered(
            @Param("bloodGroup") String bloodGroup,
            @Param("status") String status,
            Pageable pageable);

    Page<BloodRequest> findByUserId(String userId, Pageable pageable);

    Optional<BloodRequest> findByIdAndUserId(String id, String userId);

    long countByStatus(String status);
}
