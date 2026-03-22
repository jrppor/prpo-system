package com.jirapat.prpo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleResponse {

    private UUID id;
    private String name;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
