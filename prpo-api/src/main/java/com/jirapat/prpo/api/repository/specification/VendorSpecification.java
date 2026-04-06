package com.jirapat.prpo.api.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import com.jirapat.prpo.api.entity.Vendor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VendorSpecification {

    public static Specification<Vendor> hasCode(String code) {
        return (root, query, cb) -> code == null || code.isBlank()
        ? null
        : cb.equal(root.get("code"), code);
    }

    public static Specification<Vendor> searchByKeyword(String search) {
        return (root, query, cb) -> {
            if(search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern)
            );
        };
    }

    public static Specification<Vendor> hasTax(String taxId) {
        return (root, query, cb) -> taxId == null || taxId.isBlank()
        ? null
        : cb.equal(root.get("taxId"), taxId);
    }
}
