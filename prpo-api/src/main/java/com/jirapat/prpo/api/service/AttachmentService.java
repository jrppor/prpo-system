package com.jirapat.prpo.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final FileStorageService fileStorageService;
    private final FileUploadProperties uploadProperties;
    private final SecurityService securityService;

    public AttachmentResponse upload(ReferenceType referenceType, UUID referenceId, MultipartFile file) {
        log.info("Uploading attachment for {} id={}, file={}", referenceType, referenceId, file.getOriginalFilename());

        // Verify the referenced entity exists
        verifyReferenceExists(referenceType, referenceId);

        // Check max files per reference
        long currentCount = attachmentRepository.countByReferenceTypeAndReferenceId(referenceType, referenceId);
        if (currentCount >= uploadProperties.getMaxFilesPerReference()) {
            throw new BadRequestException("Maximum number of attachments ("
                    + uploadProperties.getMaxFilesPerReference() + ") reached for this " + referenceType);
        }

        // Store file on disk
        String storedName = fileStorageService.store(file);

        // Save metadata to DB
        User currentUser = securityService.getCurrentUser();
        Attachment attachment = Attachment.builder()
                .referenceType(referenceType)
                .referenceId(referenceId)
                .fileName(file.getOriginalFilename())
                .storedName(storedName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedBy(currentUser)
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        log.info("Attachment saved: id={}, storedName={}", saved.getId(), storedName);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(ReferenceType referenceType, UUID referenceId) {
        verifyReferenceExists(referenceType, referenceId);
        return attachmentRepository
                .findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(referenceType, referenceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsByReference(ReferenceType referenceType, UUID referenceId) {
        return attachmentRepository
                .findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(referenceType, referenceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Attachment findAttachmentById(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId.toString()));
    }

    @Transactional(readOnly = true)
    public Resource download(UUID attachmentId) {
        Attachment attachment = findAttachmentById(attachmentId);
        log.info("Downloading attachment: id={}, fileName={}", attachmentId, attachment.getFileName());
        return fileStorageService.load(attachment.getStoredName());
    }

    public void delete(UUID attachmentId) {
        Attachment attachment = findAttachmentById(attachmentId);
        log.info("Deleting attachment: id={}, fileName={}", attachmentId, attachment.getFileName());

        // Soft delete in DB
        attachment.softDelete();
        attachmentRepository.save(attachment);

        // Delete file from disk
        fileStorageService.delete(attachment.getStoredName());

        log.info("Attachment soft-deleted: id={}", attachmentId);
    }

    // ============ Helper Methods ============

    private void verifyReferenceExists(ReferenceType referenceType, UUID referenceId) {
        boolean exists = switch (referenceType) {
            case PURCHASE_REQUEST -> purchaseRequestRepository.existsById(referenceId);
            case PURCHASE_ORDER -> purchaseOrderRepository.existsById(referenceId);
        };
        if (!exists) {
            throw new ResourceNotFoundException(referenceType.name(), "id", referenceId.toString());
        }
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .referenceType(attachment.getReferenceType().name())
                .referenceId(attachment.getReferenceId())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .uploadedByName(attachment.getUploadedBy().getFullName())
                .uploadedById(attachment.getUploadedBy().getId())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
