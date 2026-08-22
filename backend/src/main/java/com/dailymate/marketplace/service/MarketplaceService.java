package com.dailymate.marketplace.service;

import com.dailymate.core.exception.ForbiddenException;
import com.dailymate.core.exception.NotFoundException;
import com.dailymate.marketplace.dto.request.ServiceProviderRequest;
import com.dailymate.marketplace.dto.response.ServiceProviderResponse;
import com.dailymate.marketplace.entity.ServiceProvider;
import com.dailymate.marketplace.repository.ServiceProviderRepository;
import com.dailymate.user.entity.User;
import com.dailymate.user.entity.UserRole;
import java.math.BigDecimal;
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
    public ServiceProviderResponse createProvider(String userId, ServiceProviderRequest request) {
        ServiceProvider provider = new ServiceProvider();
        provider.setUserId(userId);
        applyChanges(provider, request);
        return toResponse(providers.save(provider));
    }

    @Transactional
    public ServiceProviderResponse updateProvider(String id, User currentUser, ServiceProviderRequest request) {
        ServiceProvider provider = findProvider(id);
        verifyOwnership(provider, currentUser);
        applyChanges(provider, request);
        return toResponse(providers.save(provider));
    }

    @Transactional
    public void deleteProvider(String id, User currentUser) {
        ServiceProvider provider = findProvider(id);
        verifyOwnership(provider, currentUser);
        providers.delete(provider);
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
        electrician.setHourlyRate(new BigDecimal("60.00"));

        ServiceProvider plumber = new ServiceProvider();
        plumber.setName("FlowFix Plumbing");
        plumber.setCategory("Plumber");
        plumber.setDescription("Leak repairs, pipe replacement, bathroom fixtures, and emergency plumbing support.");
        plumber.setServiceArea("North District");
        plumber.setPhone("+1-555-0182");
        plumber.setEmail("service@flowfix.example");
        plumber.setHourlyRate(new BigDecimal("55.00"));

        ServiceProvider mechanic = new ServiceProvider();
        mechanic.setName("RoadReady Garage");
        mechanic.setCategory("Mechanic");
        mechanic.setDescription("Routine car maintenance, diagnostics, and small-engine repair services.");
        mechanic.setServiceArea("Citywide");
        mechanic.setPhone("+1-555-0183");
        mechanic.setEmail("repairs@roadready.example");
        mechanic.setHourlyRate(new BigDecimal("65.00"));

        providers.saveAll(List.of(electrician, plumber, mechanic));
    }

    private void applyChanges(ServiceProvider provider, ServiceProviderRequest request) {
        provider.setName(request.name().trim());
        provider.setCategory(request.category().trim());
        provider.setDescription(request.description().trim());
        provider.setServiceArea(request.serviceArea().trim());
        provider.setPhone(request.phone() != null ? request.phone().trim() : null);
        provider.setEmail(request.email() != null ? request.email().trim() : null);
        provider.setHourlyRate(request.hourlyRate());
    }

    private ServiceProvider findProvider(String id) {
        return providers.findById(id)
                .orElseThrow(() -> new NotFoundException("Service provider not found"));
    }

    private void verifyOwnership(ServiceProvider provider, User currentUser) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return;
        }
        if (provider.getUserId() == null || !provider.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to modify this provider");
        }
    }

    private ServiceProviderResponse toResponse(ServiceProvider provider) {
        return new ServiceProviderResponse(
                provider.getId(),
                provider.getUserId(),
                provider.getName(),
                provider.getCategory(),
                provider.getDescription(),
                provider.getServiceArea(),
                provider.getPhone(),
                provider.getEmail(),
                provider.getHourlyRate(),
                provider.getCreatedAt());
    }
}
