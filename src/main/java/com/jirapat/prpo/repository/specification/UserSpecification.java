package com.jirapat.prpo.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import com.jirapat.prpo.entity.User;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserSpecification {

    public static Specification<User> hasEmail(String email) {
        return (root, query, cb) -> email == null || email.isBlank()
            ? null
            : cb.equal(root.get("email"), email);
    }
}
