package com.dailymate.expense.service;

import com.dailymate.core.exception.NotFoundException;
import com.dailymate.expense.dto.request.ExpenseEntryRequest;
import com.dailymate.expense.dto.response.ExpenseEntryResponse;
import com.dailymate.expense.entity.ExpenseEntry;
import com.dailymate.expense.repository.ExpenseEntryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseEntryRepository expenses;

    public ExpenseService(ExpenseEntryRepository expenses) {
        this.expenses = expenses;
    }

    public List<ExpenseEntryResponse> getEntries(String userId) {
        return expenses.findByUserIdOrderBySpentOnDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ExpenseEntryResponse createEntry(String userId, ExpenseEntryRequest request) {
        ExpenseEntry entry = new ExpenseEntry();
        entry.setUserId(userId);
        applyChanges(entry, request);
        return toResponse(expenses.save(entry));
    }

    @Transactional
    public ExpenseEntryResponse updateEntry(String userId, String expenseId, ExpenseEntryRequest request) {
        ExpenseEntry entry = findEntry(userId, expenseId);
        applyChanges(entry, request);
        return toResponse(expenses.save(entry));
    }

    @Transactional
    public void deleteEntry(String userId, String expenseId) {
        ExpenseEntry entry = findEntry(userId, expenseId);
        expenses.delete(entry);
    }

    private void applyChanges(ExpenseEntry entry, ExpenseEntryRequest request) {
        entry.setCategory(request.category().trim());
        entry.setDescription(request.description().trim());
        entry.setAmount(request.amount());
        entry.setSpentOn(request.spentOn());
        entry.setNotes(request.notes() == null ? null : request.notes().trim());
    }

    private ExpenseEntry findEntry(String userId, String expenseId) {
        return expenses.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new NotFoundException("Expense entry not found"));
    }

    private ExpenseEntryResponse toResponse(ExpenseEntry entry) {
        return new ExpenseEntryResponse(
                entry.getId(),
                entry.getCategory(),
                entry.getDescription(),
                entry.getAmount(),
                entry.getSpentOn(),
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }
}
