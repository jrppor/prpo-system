package com.jirapat.prpo.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.jirapat.prpo.api.dto.response.ApprovalHistoryResponse;
import com.jirapat.prpo.api.entity.ApprovalHistory;
import com.jirapat.prpo.api.entity.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApprovalHistoryMapper {

    @Mapping(source = "approver", target = "approverName", qualifiedByName = "userToFullName")
    ApprovalHistoryResponse toResponse(ApprovalHistory approvalHistory);

    @Named("userToFullName")
    default String userToFullName(User user) {
        if (user == null) return null;
        return user.getFirstName() + " " + user.getLastName();
    }
}
