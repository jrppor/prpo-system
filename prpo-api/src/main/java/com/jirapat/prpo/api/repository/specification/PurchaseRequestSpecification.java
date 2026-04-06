package com.jirapat.prpo.api.repository.specification;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.jpa.domain.Specification;

import com.jirapat.prpo.api.entity.PurchaseRequest;
import com.jirapat.prpo.api.entity.PurchaseRequestStatus;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PurchaseRequestSpecification {

    public static Specification<PurchaseRequest> hasStatus(PurchaseRequestStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<PurchaseRequest> hasDepartment(String department) {
        return (root, query, cb) -> department == null || department.isBlank()
                ? null
                : cb.equal(root.get("department"), department);
    }

    public static Specification<PurchaseRequest> createdAfter(LocalDate dateFrom) {
        return (root, query, cb) -> dateFrom == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay());
    }

    public static Specification<PurchaseRequest> createdBefore(LocalDate dateTo) {
        return (root, query, cb) -> dateTo == null
                ? null
                : cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.atTime(LocalTime.MAX));
    }

    public static Specification<PurchaseRequest> searchByKeyword(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("prNumber")), pattern),
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("justification")), pattern)
            );
        };
    }
}
