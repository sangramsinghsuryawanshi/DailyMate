package com.dailymate.core.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dailymate.assistant.entity.AssistantConversation;
import com.dailymate.assistant.repository.AssistantConversationRepository;
import com.dailymate.expense.entity.ExpenseEntry;
import com.dailymate.expense.repository.ExpenseEntryRepository;
import com.dailymate.grocery.entity.GroceryItem;
import com.dailymate.grocery.repository.GroceryItemRepository;
import com.dailymate.lostfound.entity.LostItemPost;
import com.dailymate.lostfound.repository.LostItemPostRepository;
import com.dailymate.medicine.entity.MedicineReminder;
import com.dailymate.medicine.repository.MedicineReminderRepository;
import com.dailymate.notification.entity.Notification;
import com.dailymate.notification.repository.NotificationRepository;
import com.dailymate.user.entity.User;
import com.dailymate.user.entity.UserRole;
import com.dailymate.user.entity.UserStatus;
import com.dailymate.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseIntegrityIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MedicineReminderRepository medicineReminderRepository;

    @Autowired
    private ExpenseEntryRepository expenseEntryRepository;

    @Autowired
    private LostItemPostRepository lostItemPostRepository;

    @Autowired
    private AssistantConversationRepository assistantConversationRepository;

    @Autowired
    private GroceryItemRepository groceryItemRepository;

    @Test
    void verifiesMonetaryPrecisionPreservation() {
        GroceryItem item = new GroceryItem();
        item.setName("Organic Olive Oil");
        item.setCategory("Pantry");
        item.setStore("Green Grocer");
        item.setPrice(new BigDecimal("18.75"));
        item.setLocation("Aisle 4");
        GroceryItem savedItem = groceryItemRepository.saveAndFlush(item);

        GroceryItem fetchedItem = groceryItemRepository.findById(savedItem.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("18.75").compareTo(fetchedItem.getPrice()));

        ExpenseEntry expense = new ExpenseEntry();
        expense.setUserId("test-user-id");
        expense.setCategory("Food");
        expense.setDescription("Weekly groceries");
        expense.setAmount(new BigDecimal("125.50"));
        expense.setSpentOn(LocalDate.now());
        ExpenseEntry savedExpense = expenseEntryRepository.saveAndFlush(expense);

        ExpenseEntry fetchedExpense = expenseEntryRepository.findById(savedExpense.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("125.50").compareTo(fetchedExpense.getAmount()));
    }

    @Test
    void userOwnedEntitiesAreAssociatedAndPersistedCleanly() {
        User user = new User();
        user.setEmail("integrity@example.com");
        user.setPasswordHash("hashedpassword");
        user.setFirstName("Database");
        user.setLastName("Integrity");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.saveAndFlush(user);

        Notification notification = new Notification();
        notification.setUserId(savedUser.getId());
        notification.setTitle("Alert");
        notification.setMessage("Test notification");
        notification.setType("info");
        notificationRepository.saveAndFlush(notification);

        MedicineReminder reminder = new MedicineReminder();
        reminder.setUserId(savedUser.getId());
        reminder.setName("Vitamin C");
        reminder.setDosage("500mg");
        reminder.setFrequency("Daily");
        reminder.setRemindAt(LocalTime.of(8, 0));
        medicineReminderRepository.saveAndFlush(reminder);

        ExpenseEntry expense = new ExpenseEntry();
        expense.setUserId(savedUser.getId());
        expense.setCategory("Healthcare");
        expense.setDescription("Prescription");
        expense.setAmount(new BigDecimal("35.00"));
        expense.setSpentOn(LocalDate.now());
        expenseEntryRepository.saveAndFlush(expense);

        LostItemPost lostPost = new LostItemPost();
        lostPost.setUserId(savedUser.getId());
        lostPost.setTitle("Lost Keys");
        lostPost.setItemType("Keys");
        lostPost.setLocation("Park");
        lostPost.setDescription("Keychain with brass key");
        lostPost.setContactName("Database");
        lostPost.setContactPhone("+1-555-0199");
        lostItemPostRepository.saveAndFlush(lostPost);

        AssistantConversation conversation = new AssistantConversation();
        conversation.setUserId(savedUser.getId());
        conversation.setTitle("Session 1");
        conversation.setPrompt("Help me organize my day");
        conversation.setResponse("Here is a schedule");
        assistantConversationRepository.saveAndFlush(conversation);

        assertTrue(notificationRepository.findByUserIdOrderByCreatedAtDesc(savedUser.getId()).size() >= 1);
        assertTrue(medicineReminderRepository.findByUserIdOrderByRemindAtAsc(savedUser.getId()).size() >= 1);
        assertTrue(expenseEntryRepository.findByUserIdOrderBySpentOnDesc(savedUser.getId()).size() >= 1);
        assertTrue(lostItemPostRepository.findByUserIdOrderByCreatedAtDesc(savedUser.getId()).size() >= 1);
        assertTrue(assistantConversationRepository.findByUserIdOrderByCreatedAtDesc(savedUser.getId()).size() >= 1);
    }
}
