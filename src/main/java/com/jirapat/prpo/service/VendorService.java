package com.jirapat.prpo.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jirapat.prpo.dto.request.CreateVendorRequest;
import com.jirapat.prpo.dto.request.UpdateVendorRequest;
import com.jirapat.prpo.dto.response.VendorResponse;
import com.jirapat.prpo.entity.Vendor;
import com.jirapat.prpo.exception.DuplicateResourceException;
import com.jirapat.prpo.exception.ResourceNotFoundException;
import com.jirapat.prpo.mapper.VendorMapper;
import com.jirapat.prpo.repository.VendorRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;
    private final SecurityService securityService;
    

    public Page<VendorResponse> getAllVendors(Pageable pageable) {
        return vendorRepository.findAll(pageable)
                .map(vendorMapper::toVendorResponse);
    }

    public VendorResponse getVendorById(UUID id) {
        log.info("Fetching vendor by id: {}", id);
        Vendor vendor = findVendorById(id);
        return vendorMapper.toVendorResponse(vendor);
    }

    @Transactional
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

    @Transactional
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

    @Transactional
    public void deleteVendor(UUID id) {
        log.info("Deleting vendor: {}", id);
        Vendor vendor = findVendorById(id);
        vendorRepository.delete(vendor);
        log.info("Vendor deleted successfully: {}", id);
    }

    // ============ Helper Methods ============
    public Vendor findVendorById(UUID id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "id", id.toString()));
    }

    public Vendor findVendorByName(String name) {
        return vendorRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", "name", name));
    }
}
