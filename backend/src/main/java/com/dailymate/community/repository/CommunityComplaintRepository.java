package com.dailymate.community.repository;

import com.dailymate.community.entity.CommunityComplaint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityComplaintRepository extends JpaRepository<CommunityComplaint, String> {
    java.util.List<CommunityComplaint> findAllByOrderByCreatedAtDesc();
}
