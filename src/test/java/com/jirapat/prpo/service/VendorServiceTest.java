package com.jirapat.prpo.service;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jirapat.prpo.dto.request.CreateVendorRequest;
import com.jirapat.prpo.dto.request.UpdateVendorRequest;
import com.jirapat.prpo.dto.response.VendorResponse;
import com.jirapat.prpo.entity.Vendor;
import com.jirapat.prpo.exception.DuplicateResourceException;
import com.jirapat.prpo.exception.ResourceNotFoundException;
import com.jirapat.prpo.mapper.VendorMapper;
import com.jirapat.prpo.repository.VendorRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("VendorService Unit Tests")
class VendorServiceTest {

    @Mock private VendorRepository vendorRepository;
    @Mock private VendorMapper vendorMapper;
    @Mock private SecurityService securityService;

    @InjectMocks
    private VendorService vendorService;

    private Vendor testVendor;
    private VendorResponse testVendorResponse;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        testVendor = Vendor.builder()
                .id(vendorId)
                .code("VEN-001")
                .name("ACME CORP")
                .contactName("John")
                .email("john@acme.com")
                .isActive(true)
                .build();
        testVendorResponse = VendorResponse.builder()
                .id(vendorId)
                .code("VEN-001")
                .name("ACME CORP")
                .build();
    }

    @Nested
    @DisplayName("getVendorById()")
    class GetVendorByIdTests {

        @Test
        @DisplayName("should return vendor when found")
        void getVendorById_Found_ReturnsResponse() {
            when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(testVendor));
            when(vendorMapper.toVendorResponse(testVendor)).thenReturn(testVendorResponse);

            VendorResponse result = vendorService.getVendorById(vendorId);

            assertThat(result.getId()).isEqualTo(vendorId);
            assertThat(result.getName()).isEqualTo("ACME CORP");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void getVendorById_NotFound_Throws() {
            UUID missingId = UUID.randomUUID();
            when(vendorRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vendorService.getVendorById(missingId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createVendor()")
    class CreateVendorTests {

        @Test
        @DisplayName("should create vendor and uppercase name")
        void createVendor_Valid_ReturnsResponse() {
            CreateVendorRequest request = CreateVendorRequest.builder()
                    .code("VEN-002")
                    .name("new vendor")
                    .build();

            when(vendorRepository.existsByName("NEW VENDOR")).thenReturn(false);
            when(vendorMapper.toEntity(request)).thenReturn(testVendor);
            when(vendorRepository.save(any(Vendor.class))).thenReturn(testVendor);
            when(vendorMapper.toVendorResponse(testVendor)).thenReturn(testVendorResponse);

            VendorResponse result = vendorService.createVendor(request);

            assertThat(result).isNotNull();
            verify(vendorRepository).save(any(Vendor.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException on duplicate name")
        void createVendor_DuplicateName_Throws() {
            CreateVendorRequest request = CreateVendorRequest.builder()
                    .code("VEN-002")
                    .name("ACME CORP")
                    .build();

            when(vendorRepository.existsByName("ACME CORP")).thenReturn(true);

            assertThatThrownBy(() -> vendorService.createVendor(request))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(vendorRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateVendor()")
    class UpdateVendorTests {

        @Test
        @DisplayName("should update vendor with new name")
        void updateVendor_NewName_ReturnsUpdated() {
            UpdateVendorRequest request = UpdateVendorRequest.builder()
                    .name("new name")
                    .build();

            when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(testVendor));
            when(vendorRepository.existsByName("NEW NAME")).thenReturn(false);
            when(vendorRepository.save(any(Vendor.class))).thenReturn(testVendor);
            when(vendorMapper.toVendorResponse(testVendor)).thenReturn(testVendorResponse);

            VendorResponse result = vendorService.updateVendor(vendorId, request);

            assertThat(result).isNotNull();
            verify(vendorMapper).updateEntityFromRequest(request, testVendor);
        }

        @Test
        @DisplayName("should throw when updating to duplicate name")
        void updateVendor_DuplicateName_Throws() {
            UpdateVendorRequest request = UpdateVendorRequest.builder()
                    .name("other vendor")
                    .build();

            when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(testVendor));
            when(vendorRepository.existsByName("OTHER VENDOR")).thenReturn(true);

            assertThatThrownBy(() -> vendorService.updateVendor(vendorId, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("deleteVendor()")
    class DeleteVendorTests {

        @Test
        @DisplayName("should soft-delete vendor")
        void deleteVendor_Found_SoftDeletesSuccessfully() {
            when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(testVendor));

            vendorService.deleteVendor(vendorId);

            assertThat(testVendor.getDeletedAt()).isNotNull();
            verify(vendorRepository).save(testVendor);
        }

        @Test
        @DisplayName("should throw when vendor not found")
        void deleteVendor_NotFound_Throws() {
            UUID missingId = UUID.randomUUID();
            when(vendorRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vendorService.deleteVendor(missingId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
