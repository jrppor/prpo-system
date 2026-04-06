package com.jirapat.prpo.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VendorResponse {
        private UUID id;
        private String code;
        private String name;
        private String contactName;
        private String email;
        private String phone;
        private String address;
        private String taxId;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
}
