package com.dailymate.grocery.controller;

import com.dailymate.grocery.dto.request.GroceryItemRequest;
import com.dailymate.grocery.dto.response.GroceryItemResponse;
import com.dailymate.grocery.service.GroceryComparisonService;
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
@RequestMapping("/api/v1/grocery")
public class GroceryComparisonController {

    private final GroceryComparisonService groceryComparisonService;

    public GroceryComparisonController(GroceryComparisonService groceryComparisonService) {
        this.groceryComparisonService = groceryComparisonService;
    }

    @GetMapping("/items")
    public List<GroceryItemResponse> getItems() {
        return groceryComparisonService.getItems();
    }

    @PostMapping("/items")
    public ResponseEntity<GroceryItemResponse> createItem(@Valid @RequestBody GroceryItemRequest request) {
        GroceryItemResponse response = groceryComparisonService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/items/{id}")
    public GroceryItemResponse updateItem(@PathVariable String id, @Valid @RequestBody GroceryItemRequest request) {
        return groceryComparisonService.updateItem(id, request);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        groceryComparisonService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
