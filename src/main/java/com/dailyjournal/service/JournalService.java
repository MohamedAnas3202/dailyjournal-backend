package com.dailyjournal.service;

import com.dailyjournal.dto.JournalRequest;
import com.dailyjournal.entity.JournalEntry;
import com.dailyjournal.entity.User;
import com.dailyjournal.repository.JournalRepository;
import com.dailyjournal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepo;
    private final UserRepository userRepo;

    // 🔐 Get the current authenticated user
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new RuntimeException("Unauthorized access");
    }

    private void checkOwnershipOrAdmin(User currentUser, User resourceOwner) {
        if (!currentUser.getId().equals(resourceOwner.getId()) &&
                currentUser.getRoles().stream().noneMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            throw new RuntimeException("Forbidden: Not authorized to access this resource.");
        }
    }

    public JournalEntry create(Long userId, JournalRequest req) {
        User currentUser = getCurrentUser();
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        checkOwnershipOrAdmin(currentUser, user);

        JournalEntry entry = new JournalEntry();
        entry.setTitle(req.getTitle());
        entry.setContent(req.getContent());
        entry.setMood(req.getMood());
        entry.setDate(req.getDate());
        entry.setTags(req.getTags());
        entry.setPrivate(req.isPrivate());
        entry.setMediaPaths(req.getMediaPaths() != null ? req.getMediaPaths() : new ArrayList<>());
        entry.setUser(user);

        return journalRepo.save(entry);
    }

    public List<JournalEntry> getAllByUser(Long userId) {
        User currentUser = getCurrentUser();
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        checkOwnershipOrAdmin(currentUser, user);
        return journalRepo.findByUserId(userId);
    }

    public JournalEntry getById(Long id) {
        JournalEntry entry = journalRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Journal not found"));
        
        User currentUser = getCurrentUser();
        User journalOwner = entry.getUser();
        
        // Allow access if:
        // 1. User is the owner of the journal
        // 2. User is an admin
        // 3. Journal is public (not private)
        boolean isOwner = currentUser.getId().equals(journalOwner.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        boolean isPublic = !entry.isPrivate();
        
        if (!isOwner && !isAdmin && !isPublic) {
            throw new RuntimeException("Forbidden: Not authorized to access this private journal.");
        }
        
        return entry;
    }

    public JournalEntry update(Long id, JournalRequest req) {
        JournalEntry entry = journalRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Journal not found"));
        checkOwnershipOrAdmin(getCurrentUser(), entry.getUser());

        entry.setTitle(req.getTitle());
        entry.setContent(req.getContent());
        entry.setMood(req.getMood());
        entry.setTags(req.getTags());
        entry.setDate(req.getDate());
        entry.setPrivate(req.isPrivate());
        entry.setMediaPaths(req.getMediaPaths() != null ? req.getMediaPaths() : new ArrayList<>());

        return journalRepo.save(entry);
    }

    public void delete(Long id) {
        JournalEntry entry = journalRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Journal not found"));
        checkOwnershipOrAdmin(getCurrentUser(), entry.getUser());
        journalRepo.deleteById(id);
    }

    public List<JournalEntry> search(Long userId, String mood, String tag, LocalDate date, String sort) {
        User currentUser = getCurrentUser();
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        checkOwnershipOrAdmin(currentUser, user);

        if (mood != null) return journalRepo.findByUserIdAndMoodContainingIgnoreCase(userId, mood);
        if (tag != null) return journalRepo.findByUserIdAndTagsContainingIgnoreCase(userId, tag);
        if (date != null) return journalRepo.findByUserIdAndDate(userId, date);
        if ("asc".equalsIgnoreCase(sort)) return journalRepo.findByUserIdOrderByDateAsc(userId);
        if ("desc".equalsIgnoreCase(sort)) return journalRepo.findByUserIdOrderByDateDesc(userId);

        return journalRepo.findByUserId(userId);
    }

    public List<JournalEntry> getByDateRange(Long userId, LocalDate start, LocalDate end) {
        User currentUser = getCurrentUser();
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        checkOwnershipOrAdmin(currentUser, user);
        return journalRepo.findByUserIdAndDateBetween(userId, start, end);
    }

    public List<String> uploadMultipleMedia(Long journalId, MultipartFile[] files) {
        JournalEntry entry = journalRepo.findById(journalId)
                .orElseThrow(() -> new RuntimeException("Journal not found"));
        checkOwnershipOrAdmin(getCurrentUser(), entry.getUser());

        String uploadDir = "uploads/";
        List<String> uploadedFilenames = new ArrayList<>();

        long totalSize = 0L;
        long maxTotalSize = 10 * 1024 * 1024; // 10 MB
        long maxFileSize = 3 * 1024 * 1024;   // 3 MB
        int maxAllowedFiles = 4;

        List<String> existingPaths = entry.getMediaPaths() != null ? entry.getMediaPaths() : new ArrayList<>();
        if ((existingPaths.size() + files.length) > maxAllowedFiles) {
            throw new RuntimeException("Upload limit reached. Only " + maxAllowedFiles + " files allowed per journal.");
        }

        List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "pdf", "mp3", "wav", "ogg");

        try {
            Files.createDirectories(Paths.get(uploadDir));
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null) throw new RuntimeException("Invalid file name");

                String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
                if (!allowedExtensions.contains(extension)) {
                    throw new RuntimeException("File type not allowed: " + originalFilename);
                }

                if (file.getSize() > maxFileSize) {
                    throw new RuntimeException("File exceeds 3MB: " + originalFilename);
                }

                totalSize += file.getSize();
                if (totalSize > maxTotalSize) {
                    throw new RuntimeException("Total upload size exceeds 10MB.");
                }

                String filename = UUID.randomUUID() + "_" + originalFilename;
                Path filepath = Paths.get(uploadDir + filename);
                file.transferTo(filepath);
                uploadedFilenames.add(filename);
            }

            existingPaths.addAll(uploadedFilenames);
            entry.setMediaPaths(existingPaths);
            journalRepo.save(entry);

            return uploadedFilenames;

        } catch (IOException e) {
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    public void deleteMediaFromJournal(Long journalId, String filename) {
        JournalEntry entry = journalRepo.findById(journalId)
                .orElseThrow(() -> new RuntimeException("Journal not found"));
        checkOwnershipOrAdmin(getCurrentUser(), entry.getUser());

        List<String> mediaPaths = entry.getMediaPaths();
        if (mediaPaths == null || !mediaPaths.contains(filename)) {
            throw new RuntimeException("Media not found in this journal.");
        }

        mediaPaths.remove(filename);
        entry.setMediaPaths(mediaPaths);
        journalRepo.save(entry);

        Path filePath = Paths.get("uploads").resolve(filename);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + filename);
        }
    }

    // ===== PUBLIC JOURNAL METHODS =====
    // These methods return only public (non-private) journals for user search and viewing
    
    public List<JournalEntry> getPublicJournalsByUser(Long userId) {
        // No ownership check needed - anyone can view public journals
        return journalRepo.findByUserIdAndIsPrivateFalse(userId);
    }
    
    public List<JournalEntry> searchPublicJournals(Long userId, String mood, String tag, LocalDate date, String sort) {
        // No ownership check needed - anyone can search public journals
        if ("asc".equalsIgnoreCase(sort)) {
            return journalRepo.findByUserIdAndIsPrivateFalseOrderByDateAsc(userId);
        }
        if ("desc".equalsIgnoreCase(sort)) {
            return journalRepo.findByUserIdAndIsPrivateFalseOrderByDateDesc(userId);
        }
        return journalRepo.findByUserIdAndIsPrivateFalse(userId);
    }
    
    public List<JournalEntry> getPublicJournalsByDateRange(Long userId, LocalDate start, LocalDate end) {
        // No ownership check needed - anyone can view public journals by date range
        return journalRepo.findByUserIdAndIsPrivateFalseAndDateBetween(userId, start, end);
    }
    
    // ===== PUBLISHED JOURNAL METHODS =====
    // These methods handle published journals that any user can view
    
    public List<JournalEntry> getAllPublishedJournals() {
        // No ownership check needed - anyone can view published journals
        return journalRepo.findByIsPublishedTrueOrderByDateDesc();
    }
    
    public List<JournalEntry> searchPublishedJournals(String search, String mood, String tags, LocalDate date) {
        // No ownership check needed - anyone can search published journals
        return journalRepo.searchPublishedJournals(search, mood, tags, date);
    }
    
    public JournalEntry publishJournal(Long journalId) {
        User currentUser = getCurrentUser();
        JournalEntry journal = journalRepo.findById(journalId)
                .orElseThrow(() -> new RuntimeException("Journal not found with ID: " + journalId));
        
        // Check ownership - only owner can publish their journal
        checkOwnershipOrAdmin(currentUser, journal.getUser());
        
        journal.setPublished(true);
        journal.setEverPublished(true); // Mark as ever published for admin view
        return journalRepo.save(journal);
    }
    
    public JournalEntry unpublishJournal(Long journalId) {
        User currentUser = getCurrentUser();
        JournalEntry journal = journalRepo.findById(journalId)
                .orElseThrow(() -> new RuntimeException("Journal not found with ID: " + journalId));
        
        // Check ownership - only owner can unpublish their journal (not admin action)
        if (!journal.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied. You can only unpublish your own journals.");
        }
        
        journal.setPublished(false);
        journal.setHiddenByAdmin(false); // When user unpublishes, it's not hidden by admin
        return journalRepo.save(journal);
    }
    
    public JournalEntry hideJournalByAdmin(Long journalId) {
        User currentUser = getCurrentUser();
        // Only admins can hide journals
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new RuntimeException("Access denied. Admin role required.");
        }
        
        JournalEntry journal = journalRepo.findById(journalId)
                .orElseThrow(() -> new RuntimeException("Journal not found with ID: " + journalId));
        
        journal.setPublished(false);
        journal.setHiddenByAdmin(true); // Mark as hidden by admin
        return journalRepo.save(journal);
    }
    
    // Admin methods to get all journals that were ever published (including hidden ones)
    public List<JournalEntry> getAllEverPublishedJournalsForAdmin() {
        User currentUser = getCurrentUser();
        // Only admins can access this
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new RuntimeException("Access denied. Admin role required.");
        }
        return journalRepo.findAllEverPublishedJournalsForAdmin();
    }
    
    public List<JournalEntry> searchAllEverPublishedJournalsForAdmin(String search, String mood, String tags, LocalDate date) {
        User currentUser = getCurrentUser();
        // Only admins can access this
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new RuntimeException("Access denied. Admin role required.");
        }
        return journalRepo.searchAllEverPublishedJournalsForAdmin(search, mood, tags, date);
    }
    
    // Admin method to restore/unhide a journal (set it back to published)
    public JournalEntry restoreJournal(Long journalId) {
        User currentUser = getCurrentUser();
        // Only admins can access this
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new RuntimeException("Access denied. Admin role required.");
        }
        
        JournalEntry journal = journalRepo.findById(journalId)
                .orElseThrow(() -> new RuntimeException("Journal not found with ID: " + journalId));
        
        // Only restore journals that were ever published
        if (!journal.isEverPublished()) {
            throw new RuntimeException("Cannot restore journal that was never published");
        }
        
        journal.setPublished(true);
        journal.setHiddenByAdmin(false); // Clear admin hide flag when restoring
        return journalRepo.save(journal);
    }
}
