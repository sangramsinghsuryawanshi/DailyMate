package com.dailymate.marketplace.controller;

import com.dailymate.core.security.UserPrincipal;
import com.dailymate.marketplace.dto.request.ServiceProviderRequest;
import com.dailymate.marketplace.dto.response.ServiceProviderResponse;
import com.dailymate.marketplace.service.MarketplaceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/marketplace")
public class MarketplaceController {

    private final MarketplaceService marketplace;

    public MarketplaceController(MarketplaceService marketplace) {
        this.marketplace = marketplace;
    }

    @GetMapping("/providers")
    public List<ServiceProviderResponse> providers() {
        return marketplace.getProviders();
    }

    @GetMapping("/providers/{id}")
    public ServiceProviderResponse provider(@PathVariable String id) {
        return marketplace.getProvider(id);
    }

    @PostMapping("/providers")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceProviderResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ServiceProviderRequest request) {
        return marketplace.createProvider(principal.user().getId(), request);
    }

    @PatchMapping("/providers/{id}")
    public ServiceProviderResponse update(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ServiceProviderRequest request) {
        return marketplace.updateProvider(id, principal.user(), request);
    }

    @DeleteMapping("/providers/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        marketplace.deleteProvider(id, principal.user());
        return ResponseEntity.noContent().build();
    }
}
