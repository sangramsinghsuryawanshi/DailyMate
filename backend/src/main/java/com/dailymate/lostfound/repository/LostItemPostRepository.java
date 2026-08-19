package com.dailymate.lostfound.repository;

import com.dailymate.lostfound.entity.LostItemPost;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostItemPostRepository extends JpaRepository<LostItemPost, String> {
    List<LostItemPost> findByUserIdOrderByCreatedAtDesc(String userId);
    List<LostItemPost> findAllByOrderByCreatedAtDesc();
    Optional<LostItemPost> findByIdAndUserId(String id, String userId);
}
