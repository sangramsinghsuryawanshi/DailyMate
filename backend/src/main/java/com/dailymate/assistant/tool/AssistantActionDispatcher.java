package com.dailymate.assistant.tool;

import com.dailymate.assistant.domain.ParsedDomainModels.*;
import com.dailymate.assistant.tool.params.CreateBloodRequestParams;
import com.dailymate.assistant.tool.params.CreateEventParams;
import com.dailymate.assistant.tool.params.CreateIceContactParams;
import com.dailymate.assistant.tool.params.CreateJobParams;
import com.dailymate.assistant.tool.params.CreateNotificationParams;
import com.dailymate.assistant.tool.params.DeleteBloodRequestParams;
import com.dailymate.assistant.tool.params.DeleteExpenseParams;
import com.dailymate.assistant.tool.params.DeleteIceContactParams;
import com.dailymate.assistant.tool.params.DeleteReminderParams;
import com.dailymate.assistant.tool.params.RegisterProviderParams;
import com.dailymate.blood.dto.request.BloodRequestCreateRequest;
import com.dailymate.blood.service.BloodDonationService;
import com.dailymate.community.dto.request.CommunityComplaintRequest;
import com.dailymate.community.service.CommunityComplaintService;
import com.dailymate.core.exception.BadRequestException;
import com.dailymate.emergency.dto.request.EmergencyContactRequest;
import com.dailymate.emergency.service.EmergencyContactService;
import com.dailymate.events.dto.request.LocalEventCreateRequest;
import com.dailymate.events.service.LocalEventService;
import com.dailymate.expense.dto.request.ExpenseEntryRequest;
import com.dailymate.expense.dto.response.ExpenseEntryResponse;
import com.dailymate.expense.service.ExpenseService;
import com.dailymate.grocery.dto.request.GroceryItemRequest;
import com.dailymate.grocery.service.GroceryComparisonService;
import com.dailymate.jobs.dto.request.JobPostRequest;
import com.dailymate.jobs.service.JobPostService;
import com.dailymate.lostfound.dto.request.LostItemPostRequest;
import com.dailymate.lostfound.service.LostFoundService;
import com.dailymate.marketplace.dto.request.ServiceProviderRequest;
import com.dailymate.marketplace.dto.response.ServiceProviderResponse;
import com.dailymate.marketplace.service.MarketplaceService;
import com.dailymate.medicine.dto.request.MedicineReminderRequest;
import com.dailymate.medicine.dto.response.MedicineReminderResponse;
import com.dailymate.medicine.service.MedicineReminderService;
import com.dailymate.notification.dto.request.NotificationRequest;
import com.dailymate.notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Universal Action Dispatcher for DailyMate AI Assistant.
 * Executes validated action proposals strictly through authorized domain services.
 * Invariant: Zero direct repository mutations from assistant code.
 */
@Component
public class AssistantActionDispatcher {

    private final ExpenseService expenseService;
    private final MedicineReminderService reminderService;
    private final MarketplaceService marketplaceService;
    private final NotificationService notificationService;
    private final BloodDonationService bloodService;
    private final EmergencyContactService emergencyService;
    private final LocalEventService eventService;
    private final JobPostService jobService;
    private final LostFoundService lostFoundService;
    private final CommunityComplaintService complaintService;
    private final GroceryComparisonService groceryService;
    private final ObjectMapper objectMapper;
    private final DecimalFormat inrFormat = new DecimalFormat("₹#,##,##0.00");

    public AssistantActionDispatcher(
            ExpenseService expenseService,
            MedicineReminderService reminderService,
            MarketplaceService marketplaceService,
            NotificationService notificationService,
            BloodDonationService bloodService,
            EmergencyContactService emergencyService,
            LocalEventService eventService,
            JobPostService jobService,
            LostFoundService lostFoundService,
            CommunityComplaintService complaintService,
            GroceryComparisonService groceryService) {
        this.expenseService = expenseService;
        this.reminderService = reminderService;
        this.marketplaceService = marketplaceService;
        this.notificationService = notificationService;
        this.bloodService = bloodService;
        this.emergencyService = emergencyService;
        this.eventService = eventService;
        this.jobService = jobService;
        this.lostFoundService = lostFoundService;
        this.complaintService = complaintService;
        this.groceryService = groceryService;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public String dispatch(String userId, String actionType, String parametersJson) {
        try {
            switch (actionType) {
                // --- 1. Expense Operations ---
                case "RECORD_EXPENSE" -> {
                    RecordExpenseParams params = objectMapper.readValue(parametersJson, RecordExpenseParams.class);
                    ExpenseEntryRequest request = new ExpenseEntryRequest(
                            params.category(),
                            params.description(),
                            params.amount(),
                            params.spentOn(),
                            params.notes());
                    ExpenseEntryResponse response = expenseService.createEntry(userId, request);
                    return "Successfully recorded expense of " + inrFormat.format(response.amount())
                            + " for " + response.category() + " (" + response.description() + ").";
                }
                case "DELETE_EXPENSE" -> {
                    DeleteExpenseParams params = objectMapper.readValue(parametersJson, DeleteExpenseParams.class);
                    expenseService.deleteEntry(userId, params.expenseId());
                    return "Successfully deleted expense entry.";
                }
                case "BULK_RECORD_EXPENSES" -> {
                    List<RecordExpenseParams> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<RecordExpenseParams>>() {}
                    );
                    BigDecimal total = BigDecimal.ZERO;
                    for (RecordExpenseParams item : items) {
                        ExpenseEntryRequest req = new ExpenseEntryRequest(
                                item.category(),
                                item.description(),
                                item.amount(),
                                item.spentOn(),
                                item.notes()
                        );
                        expenseService.createEntry(userId, req);
                        if (item.amount() != null) total = total.add(item.amount());
                    }
                    return "Successfully imported " + items.size() + " expenses totaling " + inrFormat.format(total) + ".";
                }

                // --- 2. Medicine Reminder Operations ---
                case "CREATE_REMINDER" -> {
                    CreateReminderParams params = objectMapper.readValue(parametersJson, CreateReminderParams.class);
                    MedicineReminderRequest request = new MedicineReminderRequest(
                            params.name(),
                            params.dosage(),
                            params.frequency(),
                            params.remindAt(),
                            params.notes(),
                            params.active());
                    MedicineReminderResponse response = reminderService.createReminder(userId, request);
                    return "Successfully scheduled medicine reminder for " + response.name()
                            + " (" + response.dosage() + ") at " + response.remindAt() + " (" + response.frequency() + ").";
                }
                case "BULK_CREATE_MEDICINE_REMINDERS" -> {
                    List<CreateReminderParams> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<CreateReminderParams>>() {}
                    );
                    for (CreateReminderParams item : items) {
                        MedicineReminderRequest req = new MedicineReminderRequest(
                                item.name(),
                                item.dosage(),
                                item.frequency(),
                                item.remindAt(),
                                item.notes(),
                                item.active()
                        );
                        reminderService.createReminder(userId, req);
                    }
                    return "Successfully scheduled " + items.size() + " medicine reminders.";
                }
                case "DELETE_REMINDER" -> {
                    DeleteReminderParams params = objectMapper.readValue(parametersJson, DeleteReminderParams.class);
                    reminderService.deleteReminder(userId, params.reminderId());
                    return "Successfully deleted medicine reminder.";
                }

                // --- 3. Marketplace Service Provider Operations ---
                case "REGISTER_PROVIDER" -> {
                    RegisterProviderParams params = objectMapper.readValue(parametersJson, RegisterProviderParams.class);
                    String location = (params.area() != null ? params.area() + ", " : "") + (params.city() != null ? params.city() : "Local");
                    ServiceProviderRequest request = new ServiceProviderRequest(
                            params.name(),
                            params.serviceType(),
                            "Registered via DailyMate Assistant with " + params.experienceYears() + " yrs exp",
                            location,
                            params.phone(),
                            null, // email
                            BigDecimal.valueOf(300.00) // hourlyRate
                    );
                    ServiceProviderResponse response = marketplaceService.createProvider(userId, request);
                    return "Successfully registered service provider " + response.name()
                            + " (" + response.category() + ") in " + response.serviceArea() + ".";
                }
                case "BULK_REGISTER_PROVIDERS" -> {
                    List<ParsedProvider> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<ParsedProvider>>() {}
                    );
                    for (ParsedProvider p : items) {
                        ServiceProviderRequest req = new ServiceProviderRequest(
                                p.businessName(),
                                p.category(),
                                p.description() != null ? p.description() : "Registered via Assistant",
                                p.address() != null ? p.address() : "Pune, MH",
                                p.phone(),
                                p.email(),
                                BigDecimal.valueOf(300.00)
                        );
                        marketplaceService.createProvider(userId, req);
                    }
                    return "Successfully registered " + items.size() + " service providers.";
                }

                // --- 4. Notification Operations ---
                case "MARK_NOTIFICATIONS_READ" -> {
                    notificationService.markAllRead(userId);
                    return "Successfully marked all notifications as read.";
                }
                case "CREATE_NOTIFICATION" -> {
                    CreateNotificationParams params = objectMapper.readValue(parametersJson, CreateNotificationParams.class);
                    NotificationRequest request = new NotificationRequest(
                            params.title(),
                            params.message(),
                            params.type() != null ? params.type() : "INFO",
                            false,
                            null,
                            null,
                            params.link()
                    );
                    notificationService.createNotification(userId, request);
                    return "Successfully created notification: " + params.title();
                }
                case "BULK_CREATE_NOTIFICATIONS" -> {
                    List<ParsedNotification> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<ParsedNotification>>() {}
                    );
                    for (ParsedNotification item : items) {
                        NotificationRequest req = new NotificationRequest(
                                item.title(),
                                item.message(),
                                item.type() != null ? item.type() : "INFO",
                                false,
                                null,
                                null,
                                "/notifications"
                        );
                        notificationService.createNotification(userId, req);
                    }
                    return "Successfully created " + items.size() + " notifications.";
                }

                // --- 5. Blood Donation Operations ---
                case "CREATE_BLOOD_REQUEST" -> {
                    CreateBloodRequestParams params = objectMapper.readValue(parametersJson, CreateBloodRequestParams.class);
                    BloodRequestCreateRequest request = new BloodRequestCreateRequest(
                            params.patientName(),
                            params.bloodGroup(),
                            params.unitsNeeded(),
                            params.hospitalLocation(),
                            params.urgency(),
                            params.contactName(),
                            params.contactPhone(),
                            params.additionalNotes()
                    );
                    var response = bloodService.createRequest(userId, request);
                    return "Successfully created emergency blood request for " + response.patientName()
                            + " (" + response.bloodGroup() + " - " + response.unitsNeeded() + " units) at " + response.hospitalLocation() + ".";
                }
                case "BULK_CREATE_BLOOD_REQUESTS" -> {
                    List<ParsedBloodRequest> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<ParsedBloodRequest>>() {}
                    );
                    for (ParsedBloodRequest req : items) {
                        BloodRequestCreateRequest request = new BloodRequestCreateRequest(
                                req.patientName(),
                                req.bloodGroup(),
                                1,
                                req.hospital(),
                                req.urgency() != null ? req.urgency() : "HIGH",
                                req.patientName(),
                                req.contactPhone(),
                                req.notes()
                        );
                        bloodService.createRequest(userId, request);
                    }
                    return "Successfully created " + items.size() + " emergency blood requests.";
                }
                case "DELETE_BLOOD_REQUEST" -> {
                    DeleteBloodRequestParams params = objectMapper.readValue(parametersJson, DeleteBloodRequestParams.class);
                    bloodService.deleteRequest(userId, params.requestId());
                    return "Successfully cancelled blood request.";
                }

                // --- 6. Emergency ICE Contact Operations ---
                case "CREATE_ICE_CONTACT" -> {
                    CreateIceContactParams params = objectMapper.readValue(parametersJson, CreateIceContactParams.class);
                    EmergencyContactRequest request = new EmergencyContactRequest(
                            params.name(),
                            params.category() != null ? params.category() : "Family",
                            params.phone(),
                            "ICE Personal",
                            params.relationship() + (params.notes() != null ? " - " + params.notes() : "")
                    );
                    emergencyService.createContact(userId, request);
                    return "Successfully added " + params.name() + " (" + params.relationship() + ") to your emergency ICE contacts.";
                }
                case "BULK_CREATE_ICE_CONTACTS" -> {
                    List<CreateIceContactParams> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<CreateIceContactParams>>() {}
                    );
                    for (CreateIceContactParams item : items) {
                        EmergencyContactRequest request = new EmergencyContactRequest(
                                item.name(),
                                item.category() != null ? item.category() : "Family",
                                item.phone(),
                                "ICE Personal",
                                item.relationship() + (item.notes() != null ? " - " + item.notes() : "")
                        );
                        emergencyService.createContact(userId, request);
                    }
                    return "Successfully added " + items.size() + " emergency ICE contacts.";
                }
                case "DELETE_ICE_CONTACT" -> {
                    DeleteIceContactParams params = objectMapper.readValue(parametersJson, DeleteIceContactParams.class);
                    emergencyService.deleteContact(userId, params.contactId());
                    return "Successfully deleted emergency ICE contact.";
                }

                // --- 7. Community Events Operations ---
                case "CREATE_EVENT" -> {
                    CreateEventParams params = objectMapper.readValue(parametersJson, CreateEventParams.class);
                    Instant eventInstant = params.eventDate() != null
                            ? params.eventDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                            : Instant.now();
                    LocalEventCreateRequest request = new LocalEventCreateRequest(
                            params.title(),
                            params.category() != null ? params.category() : "Community",
                            params.location() != null ? params.location() : "Local Area",
                            eventInstant,
                            params.description()
                    );
                    eventService.createEvent(userId, request);
                    return "Successfully published community event: " + params.title() + " on " + params.eventDate();
                }
                case "BULK_CREATE_EVENTS" -> {
                    List<CreateEventParams> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<CreateEventParams>>() {}
                    );
                    for (CreateEventParams item : items) {
                        Instant eventInstant = item.eventDate() != null
                                ? item.eventDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                                : Instant.now();
                        LocalEventCreateRequest req = new LocalEventCreateRequest(
                                item.title(),
                                item.category() != null ? item.category() : "Community",
                                item.location() != null ? item.location() : "Local Area",
                                eventInstant,
                                item.description()
                        );
                        eventService.createEvent(userId, req);
                    }
                    return "Successfully published " + items.size() + " community events.";
                }

                // --- 8. Community Jobs Operations ---
                case "CREATE_JOB" -> {
                    CreateJobParams params = objectMapper.readValue(parametersJson, CreateJobParams.class);
                    JobPostRequest request = new JobPostRequest(
                            params.title(),
                            "General",
                            params.location(),
                            params.type() != null ? params.type() : "Full-time",
                            params.salary(),
                            params.companyName(),
                            null, // contactPhone
                            params.contactEmail(),
                            "OPEN",
                            params.description()
                    );
                    jobService.createJobPost(userId, request);
                    return "Successfully posted job opening: " + params.title() + " at " + params.companyName();
                }
                case "BULK_CREATE_JOBS" -> {
                    List<ParsedJobPost> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<ParsedJobPost>>() {}
                    );
                    for (ParsedJobPost job : items) {
                        BigDecimal sal = job.salary() != null ? new BigDecimal(job.salary().replaceAll("[^0-9.]", "")) : BigDecimal.valueOf(35000);
                        JobPostRequest req = new JobPostRequest(
                                job.title(),
                                job.category() != null ? job.category() : "Services",
                                job.location() != null ? job.location() : "Pune",
                                job.type() != null ? job.type() : "Full-time",
                                sal,
                                job.companyName() != null ? job.companyName() : "Community Employer",
                                job.contactPhone(),
                                job.contactEmail(),
                                "OPEN",
                                job.description()
                        );
                        jobService.createJobPost(userId, req);
                    }
                    return "Successfully posted " + items.size() + " job openings.";
                }

                // --- 9. Lost & Found Operations ---
                case "BULK_CREATE_LOST_ITEMS" -> {
                    List<ParsedLostItem> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<ParsedLostItem>>() {}
                    );
                    for (ParsedLostItem item : items) {
                        LostItemPostRequest req = new LostItemPostRequest(
                                item.title(),
                                item.type() != null ? item.type() : "LOST",
                                item.location() != null ? item.location() : "Local Area",
                                item.description(),
                                "Community Resident",
                                "9876543210"
                        );
                        lostFoundService.createPost(userId, req);
                    }
                    return "Successfully posted " + items.size() + " lost & found items.";
                }

                // --- 10. Community Complaints Operations ---
                case "BULK_CREATE_COMPLAINTS" -> {
                    List<ParsedComplaint> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<ParsedComplaint>>() {}
                    );
                    for (ParsedComplaint comp : items) {
                        CommunityComplaintRequest req = new CommunityComplaintRequest(
                                comp.title(),
                                comp.category() != null ? comp.category() : "Infrastructure",
                                comp.location() != null ? comp.location() : "Community Area",
                                comp.description()
                        );
                        complaintService.createComplaint(req);
                    }
                    return "Successfully submitted " + items.size() + " community complaints.";
                }

                // --- 11. Grocery Price Comparison Operations ---
                case "BULK_ADD_GROCERY" -> {
                    List<ParsedGroceryItem> items = objectMapper.readValue(
                            parametersJson,
                            new TypeReference<List<ParsedGroceryItem>>() {}
                    );
                    for (ParsedGroceryItem item : items) {
                        GroceryItemRequest req = new GroceryItemRequest(
                                item.name(),
                                item.category() != null ? item.category() : "General",
                                item.store() != null ? item.store() : "Local Store",
                                item.price(),
                                item.unit() != null ? item.unit() : "1 unit",
                                item.location() != null ? item.location() : "Pune"
                        );
                        groceryService.createItem(userId, req);
                    }
                    return "Successfully recorded " + items.size() + " grocery item prices.";
                }

                default -> throw new BadRequestException("Unsupported or unauthorized action type: " + actionType);
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to execute action parameters: " + ex.getMessage());
        }
    }
}
