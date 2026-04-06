package com.jirapat.prpo.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import com.jirapat.prpo.api.config.FileUploadProperties;
import com.jirapat.prpo.api.exception.BadRequestException;

@DisplayName("FileStorageService Unit Tests")
class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private FileUploadProperties properties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new FileUploadProperties();
        properties.setDirectory(tempDir.toString());
        properties.setMaxFileSize(10_485_760L); // 10 MB
        properties.setMaxFilesPerReference(10);
        properties.setAllowedContentTypes(java.util.Set.of(
                "application/pdf", "image/png", "image/jpeg",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel", "application/msword"));
        fileStorageService = new FileStorageService(properties);
        fileStorageService.init();
    }

    @Nested
    @DisplayName("store()")
    class StoreTests {

        @Test
        @DisplayName("should store PDF file and return UUID-based name")
        void store_ValidPdf_ReturnsStoredName() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "quotation.pdf", "application/pdf", "pdf-content".getBytes());

            String storedName = fileStorageService.store(file);

            assertThat(storedName).endsWith(".pdf");
            assertThat(storedName).hasSize(36 + 4); // UUID + ".pdf"
            assertThat(tempDir.resolve(storedName)).exists();
        }

        @Test
        @DisplayName("should store image file")
        void store_ValidImage_ReturnsStoredName() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.png", "image/png", "png-content".getBytes());

            String storedName = fileStorageService.store(file);

            assertThat(storedName).endsWith(".png");
            assertThat(tempDir.resolve(storedName)).exists();
        }

        @Test
        @DisplayName("should throw when file is empty")
        void store_EmptyFile_ThrowsBadRequest() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.pdf", "application/pdf", new byte[0]);

            assertThatThrownBy(() -> fileStorageService.store(file))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("File is empty");
        }

        @Test
        @DisplayName("should throw when file exceeds max size")
        void store_TooLarge_ThrowsBadRequest() {
            properties.setMaxFileSize(100L); // 100 bytes
            byte[] largeContent = new byte[200];
            MockMultipartFile file = new MockMultipartFile(
                    "file", "large.pdf", "application/pdf", largeContent);

            assertThatThrownBy(() -> fileStorageService.store(file))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("File size exceeds");
        }

        @Test
        @DisplayName("should throw when content type not allowed")
        void store_DisallowedType_ThrowsBadRequest() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "virus.exe", "application/x-msdownload", "exe-content".getBytes());

            assertThatThrownBy(() -> fileStorageService.store(file))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("File type not allowed");
        }
    }

    @Nested
    @DisplayName("load()")
    class LoadTests {

        @Test
        @DisplayName("should load stored file as Resource")
        void load_ExistingFile_ReturnsResource() throws IOException {
            // Store a file first
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", "content".getBytes());
            String storedName = fileStorageService.store(file);

            Resource resource = fileStorageService.load(storedName);

            assertThat(resource.exists()).isTrue();
            assertThat(resource.isReadable()).isTrue();
            try (InputStream is = resource.getInputStream()) {
                assertThat(new String(is.readAllBytes())).isEqualTo("content");
            }
        }

        @Test
        @DisplayName("should throw when file does not exist")
        void load_NonExistent_ThrowsException() {
            assertThatThrownBy(() -> fileStorageService.load("nonexistent.pdf"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("File not found");
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("should delete file from disk")
        void delete_ExistingFile_FileRemoved() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "to-delete.pdf", "application/pdf", "content".getBytes());
            String storedName = fileStorageService.store(file);
            assertThat(tempDir.resolve(storedName)).exists();

            fileStorageService.delete(storedName);

            assertThat(tempDir.resolve(storedName)).doesNotExist();
        }

        @Test
        @DisplayName("should not throw when file already deleted")
        void delete_NonExistent_NoException() {
            // Should not throw
            fileStorageService.delete("already-gone.pdf");
        }
    }

    @Nested
    @DisplayName("Security — Path Traversal Prevention")
    class PathTraversalTests {

        @Test
        @DisplayName("should reject path traversal in load")
        void load_PathTraversal_ThrowsBadRequest() {
            assertThatThrownBy(() -> fileStorageService.load("../../../etc/passwd"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid file path");
        }

        @Test
        @DisplayName("should reject path traversal in delete")
        void delete_PathTraversal_ThrowsBadRequest() {
            assertThatThrownBy(() -> fileStorageService.delete("../../../etc/passwd"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid file path");
        }
    }
}
