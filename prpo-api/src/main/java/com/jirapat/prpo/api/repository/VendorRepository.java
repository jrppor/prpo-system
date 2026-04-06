package com.jirapat.prpo.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.jirapat.prpo.api.entity.Vendor;

@Repository
public interface  VendorRepository extends JpaRepository<Vendor, UUID>, JpaSpecificationExecutor<Vendor> {

    Page<Vendor> findByIsActive(Boolean isActive, Pageable pageable);

    Page<Vendor> findAll(Specification<Vendor> spec, Pageable pageable);

    Optional<Vendor> findByName(String name);

    boolean existsByName(String name);
}
