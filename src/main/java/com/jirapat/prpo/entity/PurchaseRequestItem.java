package com.jirapat.prpo.entity;


import java.math.BigDecimal;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name="purchase_request_items")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor

public class PurchaseRequestItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @Column(nullable = false)
    private Integer itemNumber;

    @Column(nullable = false ,length = 500)
    private String description;

    @Column(nullable = false ,precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(length = 50)
    private String unit;

    @Column(precision = 15, scale = 2)
    private BigDecimal estimatedPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(length = 500)
    private String remark;

}
