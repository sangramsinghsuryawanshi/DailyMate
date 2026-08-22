package com.dailymate.grocery.controller;

import com.dailymate.core.security.UserPrincipal;
import com.dailymate.grocery.dto.request.GroceryItemRequest;
import com.dailymate.grocery.dto.response.GroceryItemResponse;
import com.dailymate.grocery.service.GroceryComparisonService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/grocery")
public class GroceryComparisonController {

    private final GroceryComparisonService groceryComparisonService;

    public GroceryComparisonController(GroceryComparisonService groceryComparisonService) {
        this.groceryComparisonService = groceryComparisonService;
    }

    @GetMapping("/items")
    public List<GroceryItemResponse> getItems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String store) {
        return groceryComparisonService.getItems(search, category, store);
    }

    @GetMapping("/my-items")
    public List<GroceryItemResponse> getMyItems(@AuthenticationPrincipal UserPrincipal principal) {
        return groceryComparisonService.getMyItems(principal.user().getId());
    }

    @PostMapping("/items")
    public ResponseEntity<GroceryItemResponse> createItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GroceryItemRequest request) {
        GroceryItemResponse response = groceryComparisonService.createItem(principal.user().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/items/{id}")
    public GroceryItemResponse updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody GroceryItemRequest request) {
        return groceryComparisonService.updateItem(principal.user().getId(), id, request);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        groceryComparisonService.deleteItem(principal.user().getId(), id);
        return ResponseEntity.noContent().build();
    }
}
