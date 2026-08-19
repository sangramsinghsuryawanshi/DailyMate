package com.dailymate.emergency.repository;

import com.dailymate.emergency.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, String> {
}
