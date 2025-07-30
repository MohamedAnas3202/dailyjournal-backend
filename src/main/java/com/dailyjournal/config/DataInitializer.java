package com.dailyjournal.config;

import com.dailyjournal.entity.Role;
import com.dailyjournal.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepo;

    @PostConstruct
    public void init() {
        if (roleRepo.findByName("ROLE_USER").isEmpty()) {
            roleRepo.save(new Role(null, "ROLE_USER"));
        }

        if (roleRepo.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepo.save(new Role(null, "ROLE_ADMIN"));
        }
    }
}
