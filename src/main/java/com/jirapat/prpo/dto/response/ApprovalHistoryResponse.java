package com.jirapat.prpo.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.jirapat.prpo.entity.ApprovalAction;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApprovalHistoryResponse {
    private UUID id;
    private String approverName;
    private ApprovalAction action;
    private String comment;
    private Integer approvalLevel;
    private LocalDateTime approvedAt;
}
