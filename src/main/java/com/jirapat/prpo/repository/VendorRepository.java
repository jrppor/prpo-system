package com.jirapat.prpo.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jirapat.prpo.entity.Vendor;

@Repository
public interface  VendorRepository extends JpaRepository<Vendor, UUID> {

    Page<Vendor> findByIsActive(Boolean isActive, Pageable pageable);

    Optional<Vendor> findByName(String name);

    boolean existsByName(String name);
}
