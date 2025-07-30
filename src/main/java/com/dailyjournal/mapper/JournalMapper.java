// src/main/java/com/dailyjournal/mapper/JournalMapper.java
package com.dailyjournal.mapper;

import com.dailyjournal.dto.JournalResponse;
import com.dailyjournal.entity.JournalEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Component
public class JournalMapper {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public JournalResponse toResponse(JournalEntry entry) {
        List<String> mediaUrls = null;
        if (entry.getMediaPaths() != null && !entry.getMediaPaths().isEmpty()) {
            mediaUrls = entry.getMediaPaths().stream()
                    .map(filename -> baseUrl + "/api/journals/media/" + filename)
                    .collect(Collectors.toList());
        }

        Long userId = null;
        String userName = null;
        String userEmail = null; // ✅ Declare variable

        if (entry.getUser() != null) {
            userId = entry.getUser().getId();
            userName = entry.getUser().getName();
            userEmail = entry.getUser().getEmail(); // ✅ include email
        }

        return new JournalResponse(
            entry.getId(), // id
            entry.getTitle(),
            entry.getContent(),
            entry.getMood(),
            entry.getTags(),
            entry.getDate(),
            entry.isPrivate(),
            entry.isPublished(),
            entry.isEverPublished(),
            entry.isHiddenByAdmin(),
            mediaUrls,
            userId,
            userName,
            userEmail
        );
    }
}

