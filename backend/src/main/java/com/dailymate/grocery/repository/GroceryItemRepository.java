package com.dailymate.grocery.repository;

import com.dailymate.grocery.entity.GroceryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, String> {

    List<GroceryItem> findAllByOrderByPriceAsc();

    List<GroceryItem> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<GroceryItem> findAllByCategoryOrderByPriceAsc(String category);

    List<GroceryItem> findAllByStoreOrderByPriceAsc(String store);

    List<GroceryItem> findAllByNameContainingIgnoreCaseOrderByPriceAsc(String name);

    Optional<GroceryItem> findByIdAndUserId(String id, String userId);
}
