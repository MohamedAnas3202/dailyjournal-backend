package com.dailyjournal.repository;

import com.dailyjournal.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByUserId(Long userId);
    List<JournalEntry> findByUserIdAndMoodContainingIgnoreCase(Long userId, String mood);
    List<JournalEntry> findByUserIdAndTagsContainingIgnoreCase(Long userId, String tag);
    List<JournalEntry> findByUserIdAndDate(Long userId, LocalDate date);
    List<JournalEntry> findByUserIdOrderByDateAsc(Long userId);
    List<JournalEntry> findByUserIdOrderByDateDesc(Long userId);
    List<JournalEntry> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    // ✅ Admin filter support
    List<JournalEntry> findByUser_NameContainingIgnoreCaseOrUser_EmailContainingIgnoreCase(String name, String email);

    @Query("""
    SELECT j FROM JournalEntry j 
    WHERE (:query IS NULL OR LOWER(j.user.name) LIKE LOWER(CONCAT('%', :query, '%')) 
                       OR LOWER(j.user.email) LIKE LOWER(CONCAT('%', :query, '%')))
      AND (:mood IS NULL OR LOWER(j.mood) LIKE LOWER(CONCAT('%', :mood, '%')))
      AND (:tags IS NULL OR LOWER(j.tags) LIKE LOWER(CONCAT('%', :tags, '%')))
      AND (:date IS NULL OR j.date = :date)
""")
    List<JournalEntry> searchAdminJournals(
            @Param("query") String query,
            @Param("mood") String mood,
            @Param("tags") String tags,
            @Param("date") LocalDate date
    );


    @Query("""
    SELECT j FROM JournalEntry j
    WHERE j.user.id = :userId
      
      AND (:mood IS NULL OR LOWER(j.mood) LIKE LOWER(CONCAT('%', :mood, '%')))
      AND (:tags IS NULL OR LOWER(j.tags) LIKE LOWER(CONCAT('%', :tags, '%')))
      AND (:date IS NULL OR j.date = :date)
      AND (
         :search IS NULL 
         OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(j.tags) LIKE LOWER(CONCAT('%', :search, '%'))
      )
""")
    List<JournalEntry> filterUserJournals(
            @Param("userId") Long userId,
            @Param("search") String search,
            @Param("mood") String mood,
            @Param("tags") String tags,
            @Param("date") LocalDate date
    );

    // Public journals search (non-private only)
    @Query("""
    SELECT j FROM JournalEntry j
    WHERE j.user.id = :userId
      AND j.isPrivate = false
      AND (:mood IS NULL OR LOWER(j.mood) LIKE LOWER(CONCAT('%', :mood, '%')))
      AND (:tags IS NULL OR LOWER(j.tags) LIKE LOWER(CONCAT('%', :tags, '%')))
      AND (:date IS NULL OR j.date = :date)
      AND (
         :search IS NULL 
         OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(j.tags) LIKE LOWER(CONCAT('%', :search, '%'))
      )
""")
    List<JournalEntry> filterPublicJournals(
            @Param("userId") Long userId,
            @Param("search") String search,
            @Param("mood") String mood,
            @Param("tags") String tags,
            @Param("date") LocalDate date
    );

    // Get only public journals for a user
    List<JournalEntry> findByUserIdAndIsPrivateFalse(Long userId);
    
    // Get public journals ordered by date
    List<JournalEntry> findByUserIdAndIsPrivateFalseOrderByDateDesc(Long userId);
    List<JournalEntry> findByUserIdAndIsPrivateFalseOrderByDateAsc(Long userId);
    
    // Get public journals by date range
    List<JournalEntry> findByUserIdAndIsPrivateFalseAndDateBetween(Long userId, LocalDate start, LocalDate end);
    
    // Published journals methods
    @Query("SELECT j FROM JournalEntry j JOIN FETCH j.user WHERE j.isPublished = true ORDER BY j.date DESC")
    List<JournalEntry> findByIsPublishedTrueOrderByDateDesc();
    
    @Query("SELECT j FROM JournalEntry j JOIN FETCH j.user WHERE j.isPublished = true")
    List<JournalEntry> findByIsPublishedTrue();
    
    // Search published journals
    @Query("""
    SELECT j FROM JournalEntry j JOIN FETCH j.user
    WHERE j.isPublished = true
      AND (:mood IS NULL OR LOWER(j.mood) LIKE LOWER(CONCAT('%', :mood, '%')))
      AND (:tags IS NULL OR LOWER(j.tags) LIKE LOWER(CONCAT('%', :tags, '%')))
      AND (:date IS NULL OR j.date = :date)
      AND (
         :search IS NULL 
         OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
         OR j.content LIKE CONCAT('%', :search, '%')
         OR LOWER(j.tags) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(j.user.name) LIKE LOWER(CONCAT('%', :search, '%'))
      )
    ORDER BY j.date DESC
""")
    List<JournalEntry> searchPublishedJournals(
            @Param("search") String search,
            @Param("mood") String mood,
            @Param("tags") String tags,
            @Param("date") LocalDate date
    );
    
    // Admin methods to fetch all journals that were ever published (including hidden ones)
    @Query("SELECT j FROM JournalEntry j JOIN FETCH j.user WHERE j.everPublished = true ORDER BY j.date DESC")
    List<JournalEntry> findAllEverPublishedJournalsForAdmin();
    
    // Search all ever-published journals for admin (including hidden ones)
    @Query("""
    SELECT j FROM JournalEntry j JOIN FETCH j.user
    WHERE j.everPublished = true
      AND (:mood IS NULL OR LOWER(j.mood) LIKE LOWER(CONCAT('%', :mood, '%')))
      AND (:tags IS NULL OR LOWER(j.tags) LIKE LOWER(CONCAT('%', :tags, '%')))
      AND (:date IS NULL OR j.date = :date)
      AND (
         :search IS NULL 
         OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
         OR j.content LIKE CONCAT('%', :search, '%')
         OR LOWER(j.tags) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(j.user.name) LIKE LOWER(CONCAT('%', :search, '%'))
      )
    ORDER BY j.date DESC
    """)
    List<JournalEntry> searchAllEverPublishedJournalsForAdmin(
            @Param("search") String search,
            @Param("mood") String mood,
            @Param("tags") String tags,
            @Param("date") LocalDate date
    );

}
