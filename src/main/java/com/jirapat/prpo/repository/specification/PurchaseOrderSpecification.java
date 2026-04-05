package com.jirapat.prpo.repository.specification;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.jpa.domain.Specification;

import com.jirapat.prpo.entity.PurchaseOrder;
import com.jirapat.prpo.entity.PurchaseOrderStatus;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PurchaseOrderSpecification {

    public static Specification<PurchaseOrder> hasStatus(PurchaseOrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<PurchaseOrder> createdAfter(LocalDate dateFrom) {
        return (root, query, cb) -> dateFrom == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay());
    }

    public static Specification<PurchaseOrder> createdBefore(LocalDate dateTo) {
        return (root, query, cb) -> dateTo == null
                ? null
                : cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.atTime(LocalTime.MAX));
    }

    public static Specification<PurchaseOrder> searchByKeyword(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.like(cb.lower(root.get("poNumber")), pattern);
        };
    }
}
