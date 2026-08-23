package com.dailymate.medicine.repository;

import com.dailymate.medicine.entity.MedicineReminder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineReminderRepository extends JpaRepository<MedicineReminder, String> {
    List<MedicineReminder> findByUserIdOrderByRemindAtAsc(String userId);
    Page<MedicineReminder> findByUserId(String userId, Pageable pageable);
    Optional<MedicineReminder> findByIdAndUserId(String id, String userId);
}
