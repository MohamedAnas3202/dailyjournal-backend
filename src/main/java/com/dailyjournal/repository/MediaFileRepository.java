package com.dailyjournal.repository;

import com.dailyjournal.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    interface MediaFileView {
        String getFilename();
        String getOriginalFilename();
        String getContentType();
        Long getFileSize();
        LocalDateTime getCreatedAt();
        byte[] getData();
    }

    Optional<MediaFile> findByFilename(String filename);
    void deleteByFilename(String filename);

    @Query("select m.filename as filename, m.originalFilename as originalFilename, m.contentType as contentType, m.fileSize as fileSize, m.createdAt as createdAt, m.data as data from MediaFile m where m.filename = :filename")
    Optional<MediaFileView> findViewByFilename(@Param("filename") String filename);
}
