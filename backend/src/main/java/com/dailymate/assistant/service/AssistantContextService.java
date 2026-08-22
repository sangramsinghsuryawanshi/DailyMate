package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.AssistantContext;
import com.dailymate.assistant.dto.AssistantContext.EmergencyContext;
import com.dailymate.assistant.dto.AssistantContext.EventContext;
import com.dailymate.assistant.dto.AssistantContext.ExpenseContext;
import com.dailymate.assistant.dto.AssistantContext.JobContext;
import com.dailymate.assistant.dto.AssistantContext.ReminderContext;
import com.dailymate.emergency.entity.EmergencyContact;
import com.dailymate.emergency.repository.EmergencyContactRepository;
import com.dailymate.events.entity.LocalEvent;
import com.dailymate.events.repository.LocalEventRepository;
import com.dailymate.expense.entity.ExpenseEntry;
import com.dailymate.expense.repository.ExpenseEntryRepository;
import com.dailymate.jobs.entity.JobPost;
import com.dailymate.jobs.entity.JobStatus;
import com.dailymate.jobs.repository.JobPostRepository;
import com.dailymate.medicine.entity.MedicineReminder;
import com.dailymate.medicine.repository.MedicineReminderRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AssistantContextService {

    private final MedicineReminderRepository medicines;
    private final ExpenseEntryRepository expenses;
    private final EmergencyContactRepository emergencyContacts;
    private final LocalEventRepository events;
    private final JobPostRepository jobs;

    public AssistantContextService(
            MedicineReminderRepository medicines,
            ExpenseEntryRepository expenses,
            EmergencyContactRepository emergencyContacts,
            LocalEventRepository events,
            JobPostRepository jobs) {
        this.medicines = medicines;
        this.expenses = expenses;
        this.emergencyContacts = emergencyContacts;
        this.events = events;
        this.jobs = jobs;
    }

    public AssistantContext buildContext(String userId) {
        // 1. Private User Medicine Reminders
        List<ReminderContext> reminderList = medicines.findByUserIdOrderByRemindAtAsc(userId).stream()
                .filter(MedicineReminder::isActive)
                .map(m -> new ReminderContext(
                        m.getName(),
                        m.getDosage(),
                        m.getRemindAt() != null ? m.getRemindAt().toString() : "Anytime",
                        m.getFrequency()))
                .toList();

        // 2. Private User Expenses
        List<ExpenseEntry> userExpenses = expenses.findByUserIdOrderBySpentOnDesc(userId);
        BigDecimal totalExpenses = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryMap = new HashMap<>();
        for (ExpenseEntry entry : userExpenses) {
            BigDecimal amt = entry.getAmount() != null ? entry.getAmount() : BigDecimal.ZERO;
            totalExpenses = totalExpenses.add(amt);
            categoryMap.merge(entry.getCategory(), amt, BigDecimal::add);
        }
        ExpenseContext expenseContext = new ExpenseContext(totalExpenses, categoryMap, userExpenses.size());

        // 3. Public Community Events
        List<EventContext> eventList = events.findAllByStatusOrderByEventDateAsc("PUBLISHED").stream()
                .map(e -> new EventContext(
                        e.getTitle(),
                        e.getCategory(),
                        e.getLocation(),
                        e.getEventDate() != null ? e.getEventDate().toString() : "TBD"))
                .toList();

        // 4. Public Open Jobs
        List<JobContext> jobList = jobs.findAllByStatusOrderByCreatedAtDesc(JobStatus.OPEN).stream()
                .map(j -> new JobContext(
                        j.getTitle(),
                        j.getType(),
                        j.getLocation(),
                        j.getSalary(),
                        j.getCompanyName()))
                .toList();

        // 5. Emergency Hotlines & Personal Contacts
        List<String> hotlines = emergencyContacts.findAllByUserIdIsNullOrderByCreatedAtDesc().stream()
                .map(c -> c.getName() + " (" + c.getCategory() + "): " + c.getPhone())
                .toList();
        int personalCount = emergencyContacts.findAllByUserIdOrderByCreatedAtDesc(userId).size();
        EmergencyContext emergencyContext = new EmergencyContext(hotlines, personalCount);

        return new AssistantContext(reminderList, expenseContext, eventList, jobList, emergencyContext);
    }
}
