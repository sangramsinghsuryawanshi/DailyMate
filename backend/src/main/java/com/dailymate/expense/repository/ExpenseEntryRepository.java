package com.dailymate.expense.repository;

import com.dailymate.expense.entity.ExpenseEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseEntryRepository extends JpaRepository<ExpenseEntry, String> {
    List<ExpenseEntry> findByUserIdOrderBySpentOnDesc(String userId);
    Page<ExpenseEntry> findByUserId(String userId, Pageable pageable);
    Optional<ExpenseEntry> findByIdAndUserId(String id, String userId);
}
