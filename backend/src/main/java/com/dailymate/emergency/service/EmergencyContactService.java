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

    public List<EmergencyContactResponse> getContacts() {
        return contacts.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public EmergencyContactResponse createContact(EmergencyContactRequest request) {
        EmergencyContact contact = new EmergencyContact();
        applyChanges(contact, request);
        return toResponse(contacts.save(contact));
    }

    @Transactional
    public EmergencyContactResponse updateContact(String contactId, EmergencyContactRequest request) {
        EmergencyContact contact = findContact(contactId);
        applyChanges(contact, request);
        return toResponse(contacts.save(contact));
    }

    @Transactional
    public void deleteContact(String contactId) {
        EmergencyContact contact = findContact(contactId);
        contacts.delete(contact);
    }

    private void applyChanges(EmergencyContact contact, EmergencyContactRequest request) {
        contact.setName(request.name().trim());
        contact.setCategory(request.category().trim());
        contact.setPhone(request.phone().trim());
        contact.setLocation(request.location().trim());
        contact.setDescription(request.description().trim());
    }

    private EmergencyContact findContact(String contactId) {
        return contacts.findById(contactId)
                .orElseThrow(() -> new NotFoundException("Emergency contact not found"));
    }

    private EmergencyContactResponse toResponse(EmergencyContact contact) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getCategory(),
                contact.getPhone(),
                contact.getLocation(),
                contact.getDescription(),
                contact.getCreatedAt());
    }
}
