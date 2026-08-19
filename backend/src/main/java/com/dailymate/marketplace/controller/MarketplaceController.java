package com.dailymate.marketplace.controller;

import com.dailymate.marketplace.dto.response.ServiceProviderResponse;
import com.dailymate.marketplace.service.MarketplaceService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
