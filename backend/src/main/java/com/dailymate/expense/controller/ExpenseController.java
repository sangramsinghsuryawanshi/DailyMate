package com.dailymate.expense.controller;

import com.dailymate.core.security.UserPrincipal;
import com.dailymate.expense.dto.request.ExpenseEntryRequest;
import com.dailymate.expense.dto.response.ExpenseEntryResponse;
import com.dailymate.expense.service.ExpenseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenses;

    public ExpenseController(ExpenseService expenses) {
        this.expenses = expenses;
    }

    @GetMapping
    public List<ExpenseEntryResponse> getEntries(@AuthenticationPrincipal UserPrincipal principal) {
        return expenses.getEntries(principal.user().getId());
    }

    @PostMapping
    public ResponseEntity<ExpenseEntryResponse> createEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseEntryRequest request) {
        ExpenseEntryResponse response = expenses.createEntry(principal.user().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ExpenseEntryResponse updateEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody ExpenseEntryRequest request) {
        return expenses.updateEntry(principal.user().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        expenses.deleteEntry(principal.user().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
