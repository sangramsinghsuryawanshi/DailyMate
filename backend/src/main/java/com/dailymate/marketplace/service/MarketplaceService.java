package com.dailymate.marketplace.service;

import com.dailymate.core.exception.NotFoundException;
import com.dailymate.marketplace.dto.response.ServiceProviderResponse;
import com.dailymate.marketplace.entity.ServiceProvider;
import com.dailymate.marketplace.repository.ServiceProviderRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceService {

    private final ServiceProviderRepository providers;

    public MarketplaceService(ServiceProviderRepository providers) {
        this.providers = providers;
    }

    public List<ServiceProviderResponse> getProviders() {
        return providers.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ServiceProviderResponse getProvider(String id) {
        return providers.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Service provider not found"));
    }

    @Transactional
    public void seedDefaults() {
        if (!providers.findAll().isEmpty()) {
            return;
        }

        ServiceProvider electrician = new ServiceProvider();
        electrician.setName("CityLine Electric");
        electrician.setCategory("Electrician");
        electrician.setDescription("Residential and commercial electrical repairs, rewiring, and maintenance.");
        electrician.setServiceArea("Downtown and Westside");
        electrician.setPhone("+1-555-0181");
        electrician.setEmail("help@citylineelectric.example");

        ServiceProvider plumber = new ServiceProvider();
        plumber.setName("FlowFix Plumbing");
        plumber.setCategory("Plumber");
        plumber.setDescription("Leak repairs, pipe replacement, bathroom fixtures, and emergency plumbing support.");
        plumber.setServiceArea("North District");
        plumber.setPhone("+1-555-0182");
        plumber.setEmail("service@flowfix.example");

        ServiceProvider mechanic = new ServiceProvider();
        mechanic.setName("RoadReady Garage");
        mechanic.setCategory("Mechanic");
        mechanic.setDescription("Routine car maintenance, diagnostics, and small-engine repair services.");
        mechanic.setServiceArea("Citywide");
        mechanic.setPhone("+1-555-0183");
        mechanic.setEmail("repairs@roadready.example");

        providers.saveAll(List.of(electrician, plumber, mechanic));
    }

    private ServiceProviderResponse toResponse(ServiceProvider provider) {
        return new ServiceProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getCategory(),
                provider.getDescription(),
                provider.getServiceArea(),
                provider.getPhone(),
                provider.getEmail());
    }
}
