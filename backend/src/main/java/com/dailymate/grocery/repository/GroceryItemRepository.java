package com.dailymate.grocery.repository;

import com.dailymate.grocery.entity.GroceryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, String> {

    List<GroceryItem> findAllByOrderByPriceAsc();

    List<GroceryItem> findAllByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT g FROM GroceryItem g WHERE " +
           "(:search IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:category IS NULL OR LOWER(g.category) = LOWER(:category)) AND " +
           "(:store IS NULL OR LOWER(g.store) = LOWER(:store))")
    Page<GroceryItem> findFiltered(
            @Param("search") String search,
            @Param("category") String category,
            @Param("store") String store,
            Pageable pageable);

    Page<GroceryItem> findByUserId(String userId, Pageable pageable);

    Optional<GroceryItem> findByIdAndUserId(String id, String userId);
}
