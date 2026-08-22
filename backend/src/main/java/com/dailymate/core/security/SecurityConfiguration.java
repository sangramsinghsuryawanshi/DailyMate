package com.dailymate.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(UserDetailsService users, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            DaoAuthenticationProvider provider,
            ApiSecurityHandlers securityHandlers) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(provider)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(securityHandlers.accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        // Public authentication endpoints
                        .requestMatchers("/api/v1/auth/**")
                        .permitAll()

                        // Public read-only marketplace endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/marketplace/providers")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/marketplace/providers/**")
                        .permitAll()

                        // Public read-only secondary resources
                        .requestMatchers(HttpMethod.GET, "/api/v1/blood/centers").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/blood/requests").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/lost-found/posts")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/emergency-contacts/contacts")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/community-complaints/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/events")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/posts")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/grocery/items")
                        .permitAll()

                        // Infrastructure / API documentation
                        .requestMatchers("/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/swagger-ui.html")
                        .permitAll()

                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // Admin moderation & governance endpoints
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
