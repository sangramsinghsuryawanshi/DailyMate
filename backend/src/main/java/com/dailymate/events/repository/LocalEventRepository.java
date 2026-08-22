package com.dailymate.events.repository;

import com.dailymate.events.entity.LocalEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalEventRepository extends JpaRepository<LocalEvent, String> {

    List<LocalEvent> findAllByOrderByEventDateAsc();

    List<LocalEvent> findAllByCategoryOrderByEventDateAsc(String category);

    List<LocalEvent> findAllByStatusOrderByEventDateAsc(String status);

    List<LocalEvent> findAllByCategoryAndStatusOrderByEventDateAsc(String category, String status);

    List<LocalEvent> findAllByUserIdOrderByEventDateAsc(String userId);

    Optional<LocalEvent> findByIdAndUserId(String id, String userId);

    long countByStatus(String status);
}
