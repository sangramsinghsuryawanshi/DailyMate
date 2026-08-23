package com.dailymate.medicine.controller;

import com.dailymate.core.dto.response.PageResponse;
import com.dailymate.core.security.UserPrincipal;
import com.dailymate.medicine.dto.request.MedicineReminderRequest;
import com.dailymate.medicine.dto.response.MedicineReminderResponse;
import com.dailymate.medicine.service.MedicineReminderService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/medicine-reminders")
public class MedicineReminderController {

    private final MedicineReminderService reminders;

    public MedicineReminderController(MedicineReminderService reminders) {
        this.reminders = reminders;
    }

    @GetMapping
    public PageResponse<MedicineReminderResponse> getReminders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reminders.getReminders(principal.user().getId(), page, size);
    }

    @PostMapping
    public ResponseEntity<MedicineReminderResponse> createReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MedicineReminderRequest request) {
        MedicineReminderResponse response = reminders.createReminder(principal.user().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public MedicineReminderResponse updateReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody MedicineReminderRequest request) {
        return reminders.updateReminder(principal.user().getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        reminders.deleteReminder(principal.user().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
