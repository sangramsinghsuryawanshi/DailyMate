package com.dailymate.marketplace;

import com.dailymate.marketplace.service.MarketplaceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketplaceDataSeeder {

    @Bean
    CommandLineRunner seedMarketplace(MarketplaceService marketplace) {
        return args -> marketplace.seedDefaults();
    }
}
