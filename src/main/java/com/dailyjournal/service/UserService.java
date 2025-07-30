package com.dailyjournal.service;

import com.dailyjournal.dto.UserUpdateRequest;
import com.dailyjournal.entity.User;
import com.dailyjournal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public String updateUser(Long userId, UserUpdateRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Optional: verify old password
        if (req.getOldPassword() != null && req.getPassword() != null) {
            if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
                throw new RuntimeException("Old password doesn't match");
            }
        }

        // Update name and email if provided
        if (req.getName() != null && !req.getName().isBlank()) {
            user.setName(req.getName());
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            user.setEmail(req.getEmail());
        }

        // Update new password if provided
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        userRepo.save(user);
        return "User profile updated successfully.";
    }

    public String updateProfilePicture(Long userId, MultipartFile file) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty or missing.");
        }

        // ✅ File size limit: 2MB
        long maxFileSize = 2 * 1024 * 1024; // 2MB in bytes
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds 2MB limit.");
        }

        // ✅ Validate file type
        String contentType = file.getContentType();
        if (!isValidImageType(contentType)) {
            throw new RuntimeException("Invalid file type. Only PNG, JPEG, JPG, WEBP are allowed.");
        }

        // Use a proper absolute path for uploads
        String uploadDir = System.getProperty("user.home") + File.separator + "daily-journal-uploads" + File.separator + "profile-photos" + File.separator;
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create upload directory: " + uploadDir);
                }
            }

            File dest = new File(uploadDir + filename);
            file.transferTo(dest);

            // Store relative path for database
            user.setProfilePicture("/profile-photos/" + filename);
            userRepo.save(user);

            return "Profile picture uploaded successfully.";
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload profile picture: " + e.getMessage());
        }
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && (
                contentType.equalsIgnoreCase("image/jpeg") ||
                        contentType.equalsIgnoreCase("image/jpg") ||
                        contentType.equalsIgnoreCase("image/png") ||
                        contentType.equalsIgnoreCase("image/webp")
        );
    }


}
