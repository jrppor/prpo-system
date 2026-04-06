package com.jirapat.prpo.api.entity;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name="vendors", indexes= {
        @Index(name = "idx_vendor_name", columnList = "name")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Vendor extends BaseEntity {

    @Column(nullable=false, length=20)
    private String code;

    @Column(nullable=false, length=255)
    private String name;

    @Column(name = "contact_name" ,length=200)
    private String contactName;

    @Column(length=255)
    private String email;

    @Column(length=20)
    private String phone;


    private String address;

    @Column(name = "tax_id", length=20)
    private String taxId;

    @Column(name = "is_active")
    @lombok.Builder.Default
    private Boolean isActive = true;
}
