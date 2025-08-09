package com.dailyjournal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Enable CORS for frontend access (React at Vercel and localhost)
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                    "https://dailyjournal-frontend.vercel.app",
                    "http://localhost:3000"
                )
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    // Serve static files from "uploads" folder at "/uploads/**" path
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/", "classpath:/static/uploads/")
                .setCachePeriod(3600); // Cache for 1 hour
        
        // Also serve profile photos
        registry.addResourceHandler("/profile-photos/**")
                .addResourceLocations("file:profile-photos/", "classpath:/static/profile-photos/")
                .setCachePeriod(3600);
    }
}
