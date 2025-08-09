package com.dailyjournal.controller;

import com.dailyjournal.dto.JournalRequest;
import com.dailyjournal.dto.JournalResponse;
import com.dailyjournal.entity.JournalEntry;
import com.dailyjournal.mapper.JournalMapper;
import com.dailyjournal.repository.JournalRepository;
import com.dailyjournal.repository.UserRepository;
import com.dailyjournal.security.JWTService;
import com.dailyjournal.service.JournalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;
    private final JournalMapper journalMapper; // ✅ Injected instance
    private final JWTService jwtService;
    private final UserRepository userRepo;
    private final JournalRepository journalRepo;

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @PostMapping("/create/{userId}")
    public ResponseEntity<JournalResponse> create(@PathVariable Long userId,
                                                  @Valid @RequestBody JournalRequest req) {
        JournalEntry entry = journalService.create(userId, req);
        return ResponseEntity.ok(journalMapper.toResponse(entry)); // ✅ instance method
    }



    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<JournalResponse>> getAll(@PathVariable Long userId, HttpServletRequest request) {
        // Extract email from JWT in request
        String email = jwtService.extractUsernameFromRequest(request);
        
        // Get current user ID from email
        Long currentUserId = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        
        List<JournalEntry> entries = journalService.getAllByUser(userId);
        
        // If the current user is not the owner of the journals and not an admin, filter out private journals
        if (!currentUserId.equals(userId) && 
            !userRepo.findById(currentUserId).orElseThrow().getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            entries = entries.stream()
                    .filter(entry -> !entry.isPrivate())
                    .collect(Collectors.toList());
        }
        
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        journalService.delete(id);
        return ResponseEntity.ok("Journal entry deleted successfully.");
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<JournalResponse> update(@PathVariable Long id, @RequestBody JournalRequest req) {
        JournalEntry entry = journalService.update(id, req);
        return ResponseEntity.ok(journalMapper.toResponse(entry)); // ✅
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<JournalResponse> getById(@PathVariable Long id) {
        JournalEntry entry = journalService.getById(id);
        return ResponseEntity.ok(journalMapper.toResponse(entry)); // ✅
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<JournalResponse>> search(
            @RequestParam Long userId,
            @RequestParam(required = false) String mood,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String sort) {

        List<JournalEntry> entries = journalService.search(userId, mood, tag, date, sort);
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse) // ✅
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/calendar")
    public ResponseEntity<List<JournalResponse>> getByDateRange(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        List<JournalEntry> entries = journalService.getByDateRange(userId, start, end);
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse) // ✅
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<String> baseAccess() {
        return ResponseEntity.ok("JWT authentication and role-based access successful.");
    }

    // ===== PUBLIC JOURNAL ENDPOINTS =====
    // These endpoints return only public journals for user search and viewing
    
    @GetMapping("/public/user/{userId}")
    public ResponseEntity<List<JournalResponse>> getPublicJournalsByUser(@PathVariable Long userId) {
        List<JournalEntry> entries = journalService.getPublicJournalsByUser(userId);
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/public/search")
    public ResponseEntity<List<JournalResponse>> searchPublicJournals(
            @RequestParam Long userId,
            @RequestParam(required = false) String mood,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String sort) {

        List<JournalEntry> entries = journalService.searchPublicJournals(userId, mood, tag, date, sort);
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/public/calendar")
    public ResponseEntity<List<JournalResponse>> getPublicJournalsByDateRange(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        List<JournalEntry> entries = journalService.getPublicJournalsByDateRange(userId, start, end);
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @CrossOrigin(origins = {"https://dailyjournal-frontend.vercel.app"})
    @GetMapping("/media/{filename:.+}")
    public ResponseEntity<Resource> getMedia(@PathVariable String filename) throws IOException {
        Path path = Paths.get("uploads").resolve(filename);
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        // Detect content type
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
        } catch (IOException e) {
            // Use default content type
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://dailyjournal-frontend.vercel.app")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                .body(resource);
    }

    @CrossOrigin(origins = {"https://dailyjournal-frontend.vercel.app"})
    @RequestMapping(value = "/media/{filename:.+}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleMediaOptions(@PathVariable String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://dailyjournal-frontend.vercel.app")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                .build();
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @PostMapping("/{journalId}/upload")
    public ResponseEntity<?> uploadMultipleFiles(@PathVariable Long journalId,
                                                 @RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body("No files provided.");
            }

            List<String> filenames = journalService.uploadMultipleMedia(journalId, files);

            List<String> urls = filenames.stream()
                    .map(name -> "/api/journals/media/" + name)
                    .toList();

            return ResponseEntity.ok(urls);
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/filter")
    public ResponseEntity<List<JournalResponse>> filterMyJournals(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String mood,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request
    ) {
        // Extract email from JWT in request
        String email = jwtService.extractUsernameFromRequest(request);

        // Get user ID from email
        Long userId = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        // Get filtered journal entries
        List<JournalEntry> entries = journalRepo.filterUserJournals(userId, search, mood, tags, date);

        // Map to DTO
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @DeleteMapping("/{journalId}/media/{filename:.+}")
    public ResponseEntity<?> deleteMedia(
            @PathVariable Long journalId,
            @PathVariable String filename
    ) {
        try {
            journalService.deleteMediaFromJournal(journalId, filename);
            return ResponseEntity.ok("Media file deleted successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Something went wrong: " + e.getMessage());
        }
    }

    // ===== PUBLISHED JOURNAL ENDPOINTS =====
    // These endpoints handle published journals that any authenticated user can view
    
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/published")
    public ResponseEntity<List<JournalResponse>> getAllPublishedJournals() {
        List<JournalEntry> entries = journalService.getAllPublishedJournals();
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/published/search")
    public ResponseEntity<List<JournalResponse>> searchPublishedJournals(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String mood,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<JournalEntry> entries = journalService.searchPublishedJournals(search, mood, tags, date);
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @PostMapping("/{journalId}/publish")
    public ResponseEntity<JournalResponse> publishJournal(@PathVariable Long journalId) {
        JournalEntry entry = journalService.publishJournal(journalId);
        return ResponseEntity.ok(journalMapper.toResponse(entry));
    }
    
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @PostMapping("/{journalId}/unpublish")
    public ResponseEntity<JournalResponse> unpublishJournal(@PathVariable Long journalId) {
        JournalEntry entry = journalService.unpublishJournal(journalId);
        return ResponseEntity.ok(journalMapper.toResponse(entry));
    }
    
    // ===== ADMIN ENDPOINTS FOR ALL EVER-PUBLISHED JOURNALS =====
    // These endpoints show all journals that were ever published (including hidden ones) for admin moderation
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/published")
    public ResponseEntity<List<JournalResponse>> getAllEverPublishedJournalsForAdmin() {
        List<JournalEntry> entries = journalService.getAllEverPublishedJournalsForAdmin();
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/published/search")
    public ResponseEntity<List<JournalResponse>> searchAllEverPublishedJournalsForAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String mood,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<JournalEntry> entries = journalService.searchAllEverPublishedJournalsForAdmin(search, mood, tags, date);
        List<JournalResponse> response = entries.stream()
                .map(journalMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{journalId}/restore")
    public ResponseEntity<JournalResponse> restoreJournal(@PathVariable Long journalId) {
        JournalEntry entry = journalService.restoreJournal(journalId);
        return ResponseEntity.ok(journalMapper.toResponse(entry));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{journalId}/hide")
    public ResponseEntity<JournalResponse> hideJournalByAdmin(@PathVariable Long journalId) {
        JournalEntry entry = journalService.hideJournalByAdmin(journalId);
        return ResponseEntity.ok(journalMapper.toResponse(entry));
    }

}
