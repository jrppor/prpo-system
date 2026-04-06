package com.jirapat.prpo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttachmentResponse {
    private UUID id;
    private String referenceType;
    private UUID referenceId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String uploadedByName;
    private UUID uploadedById;
    private LocalDateTime createdAt;
}
