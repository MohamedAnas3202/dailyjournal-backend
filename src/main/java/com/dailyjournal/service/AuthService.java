package com.dailyjournal.service;

import com.dailyjournal.dto.AuthRequest;
import com.dailyjournal.dto.AuthResponse;
import com.dailyjournal.dto.RegisterRequest;
import com.dailyjournal.entity.Role;
import com.dailyjournal.entity.User;
import com.dailyjournal.repository.RoleRepository;
import com.dailyjournal.repository.UserRepository;
import com.dailyjournal.security.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final AuthenticationManager authManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        Role userRole = roleRepo.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new HashSet<>());
        user.getRoles().add(userRole);

        userRepo.save(user);

        String token = jwtService.generateToken(user.getEmail());

        // 👇 updated to include isAdmin = false
        return new AuthResponse(token, false);
    }

    public AuthResponse login(AuthRequest request) {
        // Check if user exists and is enabled
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new DisabledException("User is blocked");
        }

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // ✅ Check if user has ROLE_ADMIN
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("ROLE_ADMIN"));

        // ✅ Generate JWT token
        String token = jwtService.generateToken(request.getEmail());

        // ✅ Return token + isAdmin flag
        return new AuthResponse(token, isAdmin);
    }

}
