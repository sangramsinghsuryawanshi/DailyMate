package com.dailymate.marketplace.repository;

import com.dailymate.marketplace.entity.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, String> {
}
