package com.jirapat.prpo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jirapat.prpo.dto.response.ApiResponse;
import com.jirapat.prpo.dto.response.AttachmentResponse;
import com.jirapat.prpo.entity.Attachment;
import com.jirapat.prpo.entity.ReferenceType;
import com.jirapat.prpo.service.AttachmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "APIs สำหรับจัดการไฟล์แนบ (ใบเสนอราคา, invoice, เอกสารประกอบ)")
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "อัปโหลดไฟล์แนบ", description = "แนบไฟล์ให้ PR หรือ PO (PDF, รูปภาพ, Excel, Word)")
    public ResponseEntity<ApiResponse<AttachmentResponse>> upload(
            @RequestParam ReferenceType referenceType,
            @RequestParam UUID referenceId,
            @RequestParam("file") MultipartFile file) {

        AttachmentResponse response = attachmentService.upload(referenceType, referenceId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "File uploaded successfully"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "ดูรายการไฟล์แนบ", description = "ดึงไฟล์แนบทั้งหมดของ PR หรือ PO")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getAttachments(
            @RequestParam ReferenceType referenceType,
            @RequestParam UUID referenceId) {

        List<AttachmentResponse> response = attachmentService.getAttachments(referenceType, referenceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "ดาวน์โหลดไฟล์", description = "ดาวน์โหลดไฟล์แนบตาม ID")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        Attachment attachment = attachmentService.findAttachmentById(id);
        Resource resource = attachmentService.download(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "ลบไฟล์แนบ", description = "Soft delete ไฟล์แนบ + ลบไฟล์จาก disk")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        attachmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted successfully"));
    }
}
