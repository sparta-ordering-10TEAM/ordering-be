package com.sparta.ordering.global.storage;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    static final String UPLOAD_BASE_PATH = "./uploads";
    static final String SERVER_BASE_URL = "http://localhost:8080";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    public String upload(MultipartFile file, UUID userId) {
        validateContentType(file);

        String ext = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(UPLOAD_BASE_PATH, "profiles", userId.toString());

        try {
            Files.createDirectories(dir);
            Path targetPath = dir.resolve(filename).toAbsolutePath();
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new ApiException(GeneralResponseCode.IMAGE_UPLOAD_FAILED);
        }

        return SERVER_BASE_URL + "/uploads/profiles/" + userId + "/" + filename;
    }

    public void delete(String imageUrl) {
        String marker = "/uploads/";
        int idx = imageUrl.indexOf(marker);
        if (idx == -1) return;
        String relativePath = imageUrl.substring(idx + marker.length());
        Path file = Paths.get(UPLOAD_BASE_PATH, relativePath);
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    public Resource loadAsResource(UUID userId, String filename) {
        Path filePath = Paths.get(UPLOAD_BASE_PATH, "profiles", userId.toString(), filename);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException(GeneralResponseCode.IMAGE_NOT_FOUND);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ApiException(GeneralResponseCode.IMAGE_NOT_FOUND);
        }
    }

    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(GeneralResponseCode.IMAGE_TYPE_INVALID);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}