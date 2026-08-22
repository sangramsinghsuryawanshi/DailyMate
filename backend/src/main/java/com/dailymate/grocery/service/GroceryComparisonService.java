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

    public List<GroceryItemResponse> getItems(String search, String category, String store) {
        String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim().toLowerCase() : null;
        String normalizedCategory = (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL"))
                ? category.trim().toLowerCase()
                : null;
        String normalizedStore = (store != null && !store.trim().isEmpty() && !store.equalsIgnoreCase("ALL"))
                ? store.trim().toLowerCase()
                : null;

        List<GroceryItem> all = groceryItems.findAllByOrderByPriceAsc();

        return all.stream()
                .filter(item -> normalizedSearch == null || item.getName().toLowerCase().contains(normalizedSearch))
                .filter(item -> normalizedCategory == null || item.getCategory().toLowerCase().equals(normalizedCategory))
                .filter(item -> normalizedStore == null || item.getStore().toLowerCase().equals(normalizedStore))
                .map(this::toResponse)
                .toList();
    }

    public List<GroceryItemResponse> getMyItems(String userId) {
        return groceryItems.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GroceryItemResponse createItem(String userId, GroceryItemRequest request) {
        GroceryItem item = new GroceryItem();
        item.setUserId(userId);
        applyChanges(item, request);
        return toResponse(groceryItems.save(item));
    }

    @Transactional
    public GroceryItemResponse updateItem(String userId, String itemId, GroceryItemRequest request) {
        GroceryItem item = groceryItems.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Grocery item not found"));

        applyChanges(item, request);
        return toResponse(groceryItems.save(item));
    }

    @Transactional
    public void deleteItem(String userId, String itemId) {
        GroceryItem item = groceryItems.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Grocery item not found"));
        groceryItems.delete(item);
    }

    private void applyChanges(GroceryItem item, GroceryItemRequest request) {
        item.setName(request.name().trim());
        item.setCategory(request.category().trim());
        item.setStore(request.store().trim());
        item.setPrice(request.price());
        item.setUnit(request.unit().trim());
        item.setLocation(request.location().trim());
    }

    private GroceryItemResponse toResponse(GroceryItem item) {
        return new GroceryItemResponse(
                item.getId(),
                item.getUserId(),
                item.getName(),
                item.getCategory(),
                item.getStore(),
                item.getPrice(),
                item.getUnit(),
                item.getLocation(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
