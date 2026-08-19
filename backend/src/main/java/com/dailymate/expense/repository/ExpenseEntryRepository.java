package com.dailymate.expense.repository;

import com.dailymate.expense.entity.ExpenseEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseEntryRepository extends JpaRepository<ExpenseEntry, String> {
    List<ExpenseEntry> findByUserIdOrderBySpentOnDesc(String userId);
    Optional<ExpenseEntry> findByIdAndUserId(String id, String userId);
}
