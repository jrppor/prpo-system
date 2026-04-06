package com.jirapat.prpo.api.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jirapat.prpo.api.dto.request.CreateVendorRequest;
import com.jirapat.prpo.api.dto.request.UpdateVendorRequest;
import com.jirapat.prpo.api.dto.response.VendorResponse;
import com.jirapat.prpo.api.entity.Vendor;
import com.jirapat.prpo.api.exception.DuplicateResourceException;
import com.jirapat.prpo.api.exception.ResourceNotFoundException;
import com.jirapat.prpo.api.mapper.VendorMapper;
import com.jirapat.prpo.api.repository.VendorRepository;
import com.jirapat.prpo.api.repository.specification.VendorSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;
    private final SecurityService securityService;

    @Transactional(readOnly = true)
    public Page<VendorResponse> getAllVendors(
        String code,
        String vendorName,
        String taxId,
        Pageable pageable) {

        Specification<Vendor> spec = Specification
            .where(VendorSpecification.hasCode(code))
            .and(VendorSpecification.searchByKeyword(vendorName))
            .and(VendorSpecification.hasTax(taxId));

        return vendorRepository.findAll(spec, pageable)
                .map(vendorMapper::toVendorResponse);
    }

    @Transactional(readOnly = true)
    public VendorResponse getVendorById(UUID id) {
        log.info("Fetching vendor by id: {}", id);
        Vendor vendor = findVendorById(id);
        return vendorMapper.toVendorResponse(vendor);
    }

    public VendorResponse createVendor(CreateVendorRequest request) {
        String vendorName = request.getName().toUpperCase().trim();
        log.info("Creating new vendor: {}", vendorName);

        if(vendorRepository.existsByName(vendorName)) {
            throw new DuplicateResourceException("Vendor", "name", vendorName);
        }

        Vendor vendor = vendorMapper.toEntity(request);
        vendor.setName(vendorName);

        Vendor savedVendor = vendorRepository.save(vendor);
        log.info("Vendor created successfully with id: {}", savedVendor.getId());
        return vendorMapper.toVendorResponse(savedVendor);
    }

    public VendorResponse updateVendor(UUID id, UpdateVendorRequest request) {
        log.info("Updating vendor: {}", id);
        Vendor vendor = findVendorById(id);

        if (request.getName() != null && !request.getName().equalsIgnoreCase(vendor.getName())
                && vendorRepository.existsByName(request.getName().toUpperCase().trim())) {
            throw new DuplicateResourceException("Vendor", "name", request.getName());
        }

        vendorMapper.updateEntityFromRequest(request, vendor);
        if (vendor.getName() != null) {
            vendor.setName(vendor.getName().toUpperCase().trim());
        }

        Vendor savedVendor = vendorRepository.save(vendor);
        log.info("Vendor updated successfully: {}", id);
        return vendorMapper.toVendorResponse(savedVendor);
    }

    public void deleteVendor(UUID id) {
        log.info("Deleting vendor: {}", id);
        Vendor vendor = findVendorById(id);
        vendor.softDelete();
        vendorRepository.save(vendor);
        log.info("Vendor soft-deleted successfully: {}", id);
    }

    // ============ Helper Methods ============
    public Vendor findVendorById(UUID id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", id.toString()));
    }
}
