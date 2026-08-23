package com.dailymate.assistant.service;

import com.dailymate.assistant.dto.AssistantContext;
import com.dailymate.assistant.dto.AssistantContext.EmergencyContext;
import com.dailymate.assistant.dto.AssistantContext.EventContext;
import com.dailymate.assistant.dto.AssistantContext.ExpenseContext;
import com.dailymate.assistant.dto.AssistantContext.JobContext;
import com.dailymate.assistant.dto.AssistantContext.ReminderContext;
import com.dailymate.emergency.dto.response.EmergencyContactResponse;
import com.dailymate.emergency.service.EmergencyContactService;
import com.dailymate.events.dto.response.LocalEventResponse;
import com.dailymate.events.service.LocalEventService;
import com.dailymate.expense.dto.response.ExpenseEntryResponse;
import com.dailymate.expense.service.ExpenseService;
import com.dailymate.jobs.dto.response.JobPostResponse;
import com.dailymate.jobs.service.JobPostService;
import com.dailymate.medicine.dto.response.MedicineReminderResponse;
import com.dailymate.medicine.service.MedicineReminderService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Aggregates read context for the AI Assistant strictly through authorized domain services.
 * Invariant: Zero direct repository or persistence layer dependencies.
 */
@Service
public class AssistantContextService {

    private final MedicineReminderService medicineService;
    private final ExpenseService expenseService;
    private final EmergencyContactService emergencyContactService;
    private final LocalEventService eventService;
    private final JobPostService jobService;

    public AssistantContextService(
            MedicineReminderService medicineService,
            ExpenseService expenseService,
            EmergencyContactService emergencyContactService,
            LocalEventService eventService,
            JobPostService jobService) {
        this.medicineService = medicineService;
        this.expenseService = expenseService;
        this.emergencyContactService = emergencyContactService;
        this.eventService = eventService;
        this.jobService = jobService;
    }

    public AssistantContext buildContext(String userId) {
        // 1. Private User Medicine Reminders (via MedicineReminderService)
        List<ReminderContext> reminderList = medicineService.getReminders(userId).stream()
                .filter(MedicineReminderResponse::active)
                .map(m -> new ReminderContext(
                        m.name(),
                        m.dosage(),
                        m.remindAt() != null ? m.remindAt().toString() : "Anytime",
                        m.frequency()))
                .toList();

        // 2. Private User Expenses (via ExpenseService)
        List<ExpenseEntryResponse> userExpenses = expenseService.getEntries(userId);
        BigDecimal totalSpent = BigDecimal.ZERO;
        Map<String, BigDecimal> breakdown = new HashMap<>();

        for (ExpenseEntryResponse entry : userExpenses) {
            BigDecimal amt = entry.amount() != null ? entry.amount() : BigDecimal.ZERO;
            totalSpent = totalSpent.add(amt);
            String cat = entry.category() != null ? entry.category() : "Other";
            breakdown.put(cat, breakdown.getOrDefault(cat, BigDecimal.ZERO).add(amt));
        }

        ExpenseContext expenseContext = new ExpenseContext(totalSpent, breakdown, userExpenses.size());

        // 3. Upcoming Community Events (via LocalEventService)
        List<LocalEventResponse> upcomingEvents = eventService.getEvents(null, "OPEN");
        List<EventContext> eventList = upcomingEvents.stream()
                .limit(5)
                .map(ev -> new EventContext(
                        ev.title(),
                        ev.category(),
                        ev.location(),
                        ev.eventDate() != null ? ev.eventDate().toString() : ""))
                .toList();

        // 4. Open Community Job Posts (via JobPostService)
        List<JobPostResponse> openJobs = jobService.getJobPosts(null, null, null, "OPEN");
        List<JobContext> jobList = openJobs.stream()
                .limit(5)
                .map(j -> new JobContext(
                        j.title(),
                        j.type(),
                        j.location(),
                        j.salary(),
                        j.companyName()))
                .toList();

        // 5. Emergency Directory (via EmergencyContactService)
        List<EmergencyContactResponse> publicHotlines = emergencyContactService.getPublicContacts(null);
        List<String> hotlineStrings = publicHotlines.stream()
                .limit(4)
                .map(h -> h.name() + ": " + h.phone())
                .toList();
        List<EmergencyContactResponse> personalContacts = emergencyContactService.getMyContacts(userId, null);
        EmergencyContext emergencyContext = new EmergencyContext(hotlineStrings, personalContacts.size());

        return new AssistantContext(
                reminderList,
                expenseContext,
                eventList,
                jobList,
                emergencyContext
        );
    }
}
