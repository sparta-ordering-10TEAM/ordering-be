package com.sparta.ordering.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final FileStorageService fileStorageService;

    @GetMapping("/uploads/profiles/{userId}/{filename}")
    public ResponseEntity<Resource> serveProfileImage(
            @PathVariable UUID userId,
            @PathVariable String filename) {
        Resource resource = fileStorageService.loadAsResource(userId, filename);
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(resource)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(resource);
    }
}