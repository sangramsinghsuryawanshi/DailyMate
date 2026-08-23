package com.dailymate.events.repository;

import com.dailymate.events.entity.LocalEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocalEventRepository extends JpaRepository<LocalEvent, String> {

    List<LocalEvent> findAllByOrderByEventDateAsc();

    List<LocalEvent> findAllByCategoryOrderByEventDateAsc(String category);

    List<LocalEvent> findAllByStatusOrderByEventDateAsc(String status);

    List<LocalEvent> findAllByCategoryAndStatusOrderByEventDateAsc(String category, String status);

    List<LocalEvent> findAllByUserIdOrderByEventDateAsc(String userId);

    @Query("SELECT e FROM LocalEvent e WHERE " +
           "(:category IS NULL OR LOWER(e.category) = LOWER(:category)) AND " +
           "(:status IS NULL OR UPPER(e.status) = UPPER(:status))")
    Page<LocalEvent> findFiltered(
            @Param("category") String category,
            @Param("status") String status,
            Pageable pageable);

    Page<LocalEvent> findByUserId(String userId, Pageable pageable);

    Optional<LocalEvent> findByIdAndUserId(String id, String userId);

    long countByStatus(String status);
}
