package com.dailyjournal.controller;

import com.dailyjournal.dto.UserDTO;
import com.dailyjournal.dto.UserUpdateRequest;
import com.dailyjournal.entity.User;
import com.dailyjournal.mapper.UserMapper;
import com.dailyjournal.repository.UserRepository;
import com.dailyjournal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepo;
    private final UserMapper userMapper;

    // ✅ 1. Get current logged-in user's profile
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userMapper.toDTO(user));
    }

    // ✅ 2. Update user profile (name, email, password)
    @PutMapping("/update")
    public ResponseEntity<String> updateUser(@AuthenticationPrincipal User user,
                                             @RequestBody UserUpdateRequest request) {
        String result = userService.updateUser(user.getId(), request);
        return ResponseEntity.ok(result);
    }

    // ✅ 3. Upload profile picture
    @PostMapping("/upload-photo")
    public ResponseEntity<String> uploadPhoto(@AuthenticationPrincipal User user,
                                              @RequestParam("file") MultipartFile file) {
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }

        String result = userService.updateProfilePicture(user.getId(), file);
        return ResponseEntity.ok(result);
    }

    // ✅ 4. Admin: Get all users (returns list of UserDTO)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userRepo.findAll();
        List<UserDTO> userDTOs = users.stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }

    // ✅ 5. Admin: Block user login (soft delete)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> blockUser(@PathVariable Long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(false);
        userRepo.save(user);
        return ResponseEntity.ok("User has been blocked from logging in.");
    }

    // ✅ 6. Serve profile photo
    @GetMapping("/profile-photo/{filename:.+}")
    public ResponseEntity<Resource> getProfilePhoto(@PathVariable String filename) {
        try {
            Path path = Paths.get("profile-photos").resolve(filename);
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ✅ 7. Admin: Toggle user status (block/unblock)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/toggle-status/{id}")
    public ResponseEntity<String> toggleUserStatus(@PathVariable Long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(!user.isEnabled()); // Toggle the enabled status
        userRepo.save(user);
        String status = user.isEnabled() ? "unblocked" : "blocked";
        return ResponseEntity.ok("User has been " + status + ".");
    }
    
    // ✅ 8. Search users by name or email
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        List<User> users = userRepo.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
        List<UserDTO> userDTOs = users.stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }
}
