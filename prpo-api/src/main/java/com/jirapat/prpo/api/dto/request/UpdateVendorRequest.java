package com.jirapat.prpo.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVendorRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 200, message = "ContactName must not exceed 200 characters")
    private String contactName;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 20, message = "Name must not exceed 20 characters")
    private String phone;

    private String address;

    @NotBlank(message = "TaxId is required")
    @Size(max = 20, message = "TaxId must not exceed 20 characters")
    private String taxId;
}
