package com.dailymate.medicine.repository;

import com.dailymate.medicine.entity.MedicineReminder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicineReminderRepository extends JpaRepository<MedicineReminder, String> {
    List<MedicineReminder> findByUserIdOrderByRemindAtAsc(String userId);
    Optional<MedicineReminder> findByIdAndUserId(String id, String userId);
}
