package com.dailymate.blood.repository;

import com.dailymate.blood.entity.DonationCenter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationCenterRepository extends JpaRepository<DonationCenter, String> {
}
