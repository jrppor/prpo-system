package com.jirapat.prpo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jirapat.prpo.config.FileUploadProperties;
import com.jirapat.prpo.exception.BadRequestException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(FileUploadProperties.class)
public class FileStorageService {

    private final FileUploadProperties uploadProperties;
    private Path uploadDir;

    @PostConstruct
    public void init() {
        this.uploadDir = Paths.get(uploadProperties.getDirectory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            log.info("Upload directory initialized: {}", uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    /**
     * Store a file on disk. Returns the generated stored name (UUID-based).
     */
    public String store(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String storedName = UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            Path targetPath = uploadDir.resolve(storedName).normalize();

            // Path traversal prevention
            if (!targetPath.startsWith(uploadDir)) {
                throw new BadRequestException("Cannot store file outside upload directory");
            }

            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored: {} -> {}", originalFilename, storedName);
            return storedName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    /**
     * Load a stored file as a Resource for download.
     */
    public Resource load(String storedName) {
        try {
            Path filePath = uploadDir.resolve(storedName).normalize();

            // Path traversal prevention
            if (!filePath.startsWith(uploadDir)) {
                throw new BadRequestException("Invalid file path");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found or not readable: " + storedName);
            }
            return resource;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file: " + storedName, e);
        }
    }

    /**
     * Delete a stored file from disk.
     */
    public void delete(String storedName) {
        try {
            Path filePath = uploadDir.resolve(storedName).normalize();

            // Path traversal prevention
            if (!filePath.startsWith(uploadDir)) {
                throw new BadRequestException("Invalid file path");
            }

            Files.deleteIfExists(filePath);
            log.info("File deleted from disk: {}", storedName);
        } catch (IOException e) {
            log.warn("Failed to delete file from disk: {}", storedName, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > uploadProperties.getMaxFileSize()) {
            throw new BadRequestException("File size exceeds maximum allowed size of "
                    + (uploadProperties.getMaxFileSize() / 1024 / 1024) + " MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !uploadProperties.getAllowedContentTypes().contains(contentType)) {
            throw new BadRequestException("File type not allowed: " + contentType
                    + ". Allowed types: " + uploadProperties.getAllowedContentTypes());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
