package com.dailymate.grocery.repository;

import com.dailymate.grocery.entity.GroceryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, String> {
}
