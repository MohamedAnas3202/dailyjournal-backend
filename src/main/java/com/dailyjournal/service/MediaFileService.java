package com.dailyjournal.service;

import com.dailyjournal.entity.MediaFile;
import com.dailyjournal.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaFileService {

    private final MediaFileRepository mediaFileRepository;

    public String uploadFile(MultipartFile file) throws IOException {
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            originalFilename = "upload-" + UUID.randomUUID();
        }
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Create MediaFile entity
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFilename(uniqueFilename);
        mediaFile.setOriginalFilename(originalFilename);
        mediaFile.setContentType(file.getContentType());
        mediaFile.setFileSize(file.getSize());
        mediaFile.setData(file.getBytes());

        // Save to database
        mediaFileRepository.save(mediaFile);

        // Return the filename (which will be used in URLs)
        return uniqueFilename;
    }

    public MediaFile getFile(String filename) {
        return mediaFileRepository.findByFilename(filename).orElse(null);
    }

    public MediaFileRepository.MediaFileView getFileView(String filename) {
        return mediaFileRepository.findViewByFilename(filename).orElse(null);
    }

    public void deleteFile(String filename) {
        mediaFileRepository.deleteByFilename(filename);
    }
}
