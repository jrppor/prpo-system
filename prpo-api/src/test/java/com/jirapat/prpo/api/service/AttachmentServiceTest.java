package com.jirapat.prpo.api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.jirapat.prpo.api.config.FileUploadProperties;
import com.jirapat.prpo.api.dto.response.AttachmentResponse;
import com.jirapat.prpo.api.entity.Attachment;
import com.jirapat.prpo.api.entity.ReferenceType;
import com.jirapat.prpo.api.entity.User;
import com.jirapat.prpo.api.exception.BadRequestException;
import com.jirapat.prpo.api.exception.ResourceNotFoundException;
import com.jirapat.prpo.api.repository.AttachmentRepository;
import com.jirapat.prpo.api.repository.PurchaseOrderRepository;
import com.jirapat.prpo.api.repository.PurchaseRequestRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentService Unit Tests")
class AttachmentServiceTest {

    @Mock private AttachmentRepository attachmentRepository;
    @Mock private PurchaseRequestRepository purchaseRequestRepository;
    @Mock private PurchaseOrderRepository purchaseOrderRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private FileUploadProperties uploadProperties;
    @Mock private SecurityService securityService;

    @InjectMocks
    private AttachmentService attachmentService;

    private UUID prId;
    private UUID poId;
    private UUID attachmentId;
    private UUID userId;
    private User testUser;
    private Attachment testAttachment;

    @BeforeEach
    void setUp() {
        prId = UUID.randomUUID();
        poId = UUID.randomUUID();
        attachmentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .firstName("Admin")
                .lastName("User")
                .email("admin@prpo.com")
                .build();

        testAttachment = Attachment.builder()
                .id(attachmentId)
                .referenceType(ReferenceType.PURCHASE_REQUEST)
                .referenceId(prId)
                .fileName("quotation.pdf")
                .storedName("abc123.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .uploadedBy(testUser)
                .build();
    }

    @Nested
    @DisplayName("upload()")
    class UploadTests {

        @Test
        @DisplayName("should upload file for existing PR")
        void upload_ValidPrFile_ReturnsResponse() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "quotation.pdf", "application/pdf", "pdf-data".getBytes());

            when(purchaseRequestRepository.existsById(prId)).thenReturn(true);
            when(attachmentRepository.countByReferenceTypeAndReferenceId(ReferenceType.PURCHASE_REQUEST, prId)).thenReturn(0L);
            when(uploadProperties.getMaxFilesPerReference()).thenReturn(10);
            when(fileStorageService.store(file)).thenReturn("stored-uuid.pdf");
            when(securityService.getCurrentUser()).thenReturn(testUser);
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> {
                Attachment a = inv.getArgument(0);
                a.setId(attachmentId);
                return a;
            });

            AttachmentResponse result = attachmentService.upload(ReferenceType.PURCHASE_REQUEST, prId, file);

            assertThat(result.getFileName()).isEqualTo("quotation.pdf");
            assertThat(result.getReferenceType()).isEqualTo("PURCHASE_REQUEST");
            assertThat(result.getReferenceId()).isEqualTo(prId);
            assertThat(result.getUploadedByName()).isEqualTo("Admin User");
            verify(fileStorageService).store(file);
            verify(attachmentRepository).save(any(Attachment.class));
        }

        @Test
        @DisplayName("should upload file for existing PO")
        void upload_ValidPoFile_ReturnsResponse() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "invoice.pdf", "application/pdf", "pdf-data".getBytes());

            when(purchaseOrderRepository.existsById(poId)).thenReturn(true);
            when(attachmentRepository.countByReferenceTypeAndReferenceId(ReferenceType.PURCHASE_ORDER, poId)).thenReturn(0L);
            when(uploadProperties.getMaxFilesPerReference()).thenReturn(10);
            when(fileStorageService.store(file)).thenReturn("stored-uuid.pdf");
            when(securityService.getCurrentUser()).thenReturn(testUser);
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> {
                Attachment a = inv.getArgument(0);
                a.setId(attachmentId);
                return a;
            });

            AttachmentResponse result = attachmentService.upload(ReferenceType.PURCHASE_ORDER, poId, file);

            assertThat(result.getReferenceType()).isEqualTo("PURCHASE_ORDER");
            verify(purchaseOrderRepository).existsById(poId);
        }

        @Test
        @DisplayName("should throw when PR not found")
        void upload_PrNotFound_ThrowsNotFound() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", "data".getBytes());
            when(purchaseRequestRepository.existsById(prId)).thenReturn(false);

            assertThatThrownBy(() -> attachmentService.upload(ReferenceType.PURCHASE_REQUEST, prId, file))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(fileStorageService, never()).store(any());
        }

        @Test
        @DisplayName("should throw when max files reached")
        void upload_MaxFilesReached_ThrowsBadRequest() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", "data".getBytes());
            when(purchaseRequestRepository.existsById(prId)).thenReturn(true);
            when(attachmentRepository.countByReferenceTypeAndReferenceId(ReferenceType.PURCHASE_REQUEST, prId)).thenReturn(10L);
            when(uploadProperties.getMaxFilesPerReference()).thenReturn(10);

            assertThatThrownBy(() -> attachmentService.upload(ReferenceType.PURCHASE_REQUEST, prId, file))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Maximum number of attachments");
            verify(fileStorageService, never()).store(any());
        }
    }

    @Nested
    @DisplayName("getAttachments()")
    class GetAttachmentsTests {

        @Test
        @DisplayName("should return attachments for PR")
        void getAttachments_ValidPr_ReturnsList() {
            when(purchaseRequestRepository.existsById(prId)).thenReturn(true);
            when(attachmentRepository.findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
                    ReferenceType.PURCHASE_REQUEST, prId))
                    .thenReturn(List.of(testAttachment));

            List<AttachmentResponse> result = attachmentService.getAttachments(ReferenceType.PURCHASE_REQUEST, prId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFileName()).isEqualTo("quotation.pdf");
        }

        @Test
        @DisplayName("should return empty list when no attachments")
        void getAttachments_NoAttachments_ReturnsEmpty() {
            when(purchaseRequestRepository.existsById(prId)).thenReturn(true);
            when(attachmentRepository.findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
                    ReferenceType.PURCHASE_REQUEST, prId))
                    .thenReturn(List.of());

            List<AttachmentResponse> result = attachmentService.getAttachments(ReferenceType.PURCHASE_REQUEST, prId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAttachmentById()")
    class FindByIdTests {

        @Test
        @DisplayName("should return attachment when found")
        void findById_Found_ReturnsAttachment() {
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(testAttachment));

            Attachment result = attachmentService.findAttachmentById(attachmentId);

            assertThat(result.getId()).isEqualTo(attachmentId);
            assertThat(result.getFileName()).isEqualTo("quotation.pdf");
        }

        @Test
        @DisplayName("should throw when not found")
        void findById_NotFound_ThrowsException() {
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attachmentService.findAttachmentById(attachmentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("should soft delete attachment and remove file")
        void delete_Valid_SoftDeletesAndRemovesFile() {
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(testAttachment));

            attachmentService.delete(attachmentId);

            assertThat(testAttachment.getDeletedAt()).isNotNull();
            verify(attachmentRepository).save(testAttachment);
            verify(fileStorageService).delete("abc123.pdf");
        }

        @Test
        @DisplayName("should throw when attachment not found")
        void delete_NotFound_ThrowsException() {
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attachmentService.delete(attachmentId))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(fileStorageService, never()).delete(any());
        }
    }
}

