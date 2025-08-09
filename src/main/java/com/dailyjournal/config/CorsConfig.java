package com.dailyjournal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        
        // Allow your Vercel frontend domains
        config.addAllowedOrigin("https://dailyjournal-frontend.vercel.app");
        config.addAllowedOrigin("https://dailyjournal-frontend-git-main-mohamedanas3202s-projects.vercel.app");
        config.addAllowedOrigin("http://localhost:3000"); // For local development
        
        // Use allowedOriginPatterns for wildcard support with credentials
        config.addAllowedOriginPattern("https://*.vercel.app");
        config.addAllowedOriginPattern("https://*.onrender.com");
        
        // Allow all headers
        config.addAllowedHeader("*");
        
        // Allow all HTTP methods including OPTIONS for preflight
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        
        // Expose headers that the frontend might need
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        
        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
