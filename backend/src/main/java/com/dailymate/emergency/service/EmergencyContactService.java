package com.dailymate.emergency.service;

import com.dailymate.core.exception.NotFoundException;
import com.dailymate.emergency.dto.request.EmergencyContactRequest;
import com.dailymate.emergency.dto.response.EmergencyContactResponse;
import com.dailymate.emergency.entity.EmergencyContact;
import com.dailymate.emergency.repository.EmergencyContactRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmergencyContactService {

    private final EmergencyContactRepository contacts;

    public EmergencyContactService(EmergencyContactRepository contacts) {
        this.contacts = contacts;
    }

    public List<EmergencyContactResponse> getPublicContacts(String category) {
        String normalizedCategory = (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL"))
                ? category.trim()
                : null;

        List<EmergencyContact> result = (normalizedCategory != null)
                ? contacts.findAllByUserIdIsNullAndCategoryOrderByCreatedAtDesc(normalizedCategory)
                : contacts.findAllByUserIdIsNullOrderByCreatedAtDesc();

        return result.stream().map(this::toResponse).toList();
    }

    public List<EmergencyContactResponse> getMyContacts(String userId, String category) {
        String normalizedCategory = (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL"))
                ? category.trim()
                : null;

        List<EmergencyContact> result = (normalizedCategory != null)
                ? contacts.findAllByUserIdAndCategoryOrderByCreatedAtDesc(userId, normalizedCategory)
                : contacts.findAllByUserIdOrderByCreatedAtDesc(userId);

        return result.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmergencyContactResponse createContact(String userId, EmergencyContactRequest request) {
        EmergencyContact contact = new EmergencyContact();
        contact.setUserId(userId);
        applyChanges(contact, request);
        return toResponse(contacts.save(contact));
    }

    @Transactional
    public EmergencyContactResponse updateContact(String userId, String contactId, EmergencyContactRequest request) {
        EmergencyContact contact = contacts.findByIdAndUserId(contactId, userId)
                .orElseThrow(() -> new NotFoundException("Emergency contact not found"));

        applyChanges(contact, request);
        return toResponse(contacts.save(contact));
    }

    @Transactional
    public void deleteContact(String userId, String contactId) {
        EmergencyContact contact = contacts.findByIdAndUserId(contactId, userId)
                .orElseThrow(() -> new NotFoundException("Emergency contact not found"));
        contacts.delete(contact);
    }

    private void applyChanges(EmergencyContact contact, EmergencyContactRequest request) {
        contact.setName(request.name().trim());
        contact.setCategory(request.category().trim());
        contact.setPhone(request.phone().trim());
        contact.setLocation(request.location().trim());
        contact.setDescription(request.description().trim());
    }

    private EmergencyContactResponse toResponse(EmergencyContact contact) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getUserId(),
                contact.getName(),
                contact.getCategory(),
                contact.getPhone(),
                contact.getLocation(),
                contact.getDescription(),
                contact.getCreatedAt());
    }
}
