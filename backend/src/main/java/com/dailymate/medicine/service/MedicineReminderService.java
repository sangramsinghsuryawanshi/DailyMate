package com.dailymate.medicine.service;

import com.dailymate.core.exception.NotFoundException;
import com.dailymate.medicine.dto.request.MedicineReminderRequest;
import com.dailymate.medicine.dto.response.MedicineReminderResponse;
import com.dailymate.medicine.entity.MedicineReminder;
import com.dailymate.medicine.repository.MedicineReminderRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicineReminderService {

    private final MedicineReminderRepository reminders;

    public MedicineReminderService(MedicineReminderRepository reminders) {
        this.reminders = reminders;
    }

    public List<MedicineReminderResponse> getReminders(String userId) {
        return reminders.findByUserIdOrderByRemindAtAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public MedicineReminderResponse createReminder(String userId, MedicineReminderRequest request) {
        MedicineReminder reminder = new MedicineReminder();
        reminder.setUserId(userId);
        applyChanges(reminder, request);
        return toResponse(reminders.save(reminder));
    }

    @Transactional
    public MedicineReminderResponse updateReminder(String userId, String reminderId, MedicineReminderRequest request) {
        MedicineReminder reminder = findReminder(userId, reminderId);
        applyChanges(reminder, request);
        return toResponse(reminders.save(reminder));
    }

    @Transactional
    public void deleteReminder(String userId, String reminderId) {
        MedicineReminder reminder = findReminder(userId, reminderId);
        reminders.delete(reminder);
    }

    private void applyChanges(MedicineReminder reminder, MedicineReminderRequest request) {
        reminder.setName(request.name().trim());
        reminder.setDosage(request.dosage().trim());
        reminder.setFrequency(request.frequency().trim());
        reminder.setRemindAt(request.remindAt());
        reminder.setNotes(request.notes() == null ? null : request.notes().trim());
        reminder.setActive(request.active());
    }

    private MedicineReminder findReminder(String userId, String reminderId) {
        return reminders.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> new NotFoundException("Medicine reminder not found"));
    }

    private MedicineReminderResponse toResponse(MedicineReminder reminder) {
        return new MedicineReminderResponse(
                reminder.getId(),
                reminder.getName(),
                reminder.getDosage(),
                reminder.getFrequency(),
                reminder.getRemindAt(),
                reminder.getNotes(),
                reminder.isActive(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt());
    }
}
