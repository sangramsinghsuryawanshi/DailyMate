package com.dailymate.emergency.controller;

import com.dailymate.emergency.dto.request.EmergencyContactRequest;
import com.dailymate.emergency.dto.response.EmergencyContactResponse;
import com.dailymate.emergency.service.EmergencyContactService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emergency-contacts")
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    public EmergencyContactController(EmergencyContactService emergencyContactService) {
        this.emergencyContactService = emergencyContactService;
    }

    @GetMapping("/contacts")
    public List<EmergencyContactResponse> getContacts() {
        return emergencyContactService.getContacts();
    }

    @PostMapping("/contacts")
    public ResponseEntity<EmergencyContactResponse> createContact(@Valid @RequestBody EmergencyContactRequest request) {
        EmergencyContactResponse response = emergencyContactService.createContact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/contacts/{id}")
    public EmergencyContactResponse updateContact(@PathVariable String id, @Valid @RequestBody EmergencyContactRequest request) {
        return emergencyContactService.updateContact(id, request);
    }

    @DeleteMapping("/contacts/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable String id) {
        emergencyContactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }
}
