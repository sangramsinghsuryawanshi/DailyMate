package com.dailymate.grocery.service;

import com.dailymate.core.exception.NotFoundException;
import com.dailymate.grocery.dto.request.GroceryItemRequest;
import com.dailymate.grocery.dto.response.GroceryItemResponse;
import com.dailymate.grocery.entity.GroceryItem;
import com.dailymate.grocery.repository.GroceryItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroceryComparisonService {

    private final GroceryItemRepository groceryItems;

    public GroceryComparisonService(GroceryItemRepository groceryItems) {
        this.groceryItems = groceryItems;
    }

    public List<GroceryItemResponse> getItems() {
        return groceryItems.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public GroceryItemResponse createItem(GroceryItemRequest request) {
        GroceryItem item = new GroceryItem();
        applyChanges(item, request);
        return toResponse(groceryItems.save(item));
    }

    @Transactional
    public GroceryItemResponse updateItem(String itemId, GroceryItemRequest request) {
        GroceryItem item = findItem(itemId);
        applyChanges(item, request);
        return toResponse(groceryItems.save(item));
    }

    @Transactional
    public void deleteItem(String itemId) {
        GroceryItem item = findItem(itemId);
        groceryItems.delete(item);
    }

    private void applyChanges(GroceryItem item, GroceryItemRequest request) {
        item.setName(request.name().trim());
        item.setCategory(request.category().trim());
        item.setStore(request.store().trim());
        item.setPrice(request.price());
        item.setLocation(request.location().trim());
    }

    private GroceryItem findItem(String itemId) {
        return groceryItems.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Grocery item not found"));
    }

    private GroceryItemResponse toResponse(GroceryItem item) {
        return new GroceryItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getStore(),
                item.getPrice(),
                item.getLocation(),
                item.getCreatedAt());
    }
}
