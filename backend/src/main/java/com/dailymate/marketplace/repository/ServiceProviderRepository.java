package com.dailymate.marketplace.repository;

import com.dailymate.marketplace.entity.ServiceProvider;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, String> {
    List<ServiceProvider> findByUserId(String userId);

    @Query("SELECT p FROM ServiceProvider p WHERE " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.category) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:category IS NULL OR LOWER(p.category) = LOWER(:category))")
    Page<ServiceProvider> findFiltered(
            @Param("search") String search,
            @Param("category") String category,
            Pageable pageable);
}
