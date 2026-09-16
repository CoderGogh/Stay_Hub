package com.example.stay_hub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 자사 표준 숙소.
 * 공급사가 다르면 같은 숙소라도 별개 레코드 (매핑 병합은 비범위).
 */
@Entity
@Table(
        name = "accommodation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_accommodation_supplier_hotel",
                columnNames = {"supplier_code", "supplier_hotel_code"}
        )
)
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_code", nullable = false, length = 20, updatable = false)
    private SupplierCode supplierCode;

    // 공급사 쪽 숙소 식별자 (Supplier A: hotelCode / Supplier B: propertyId)
    @Column(name = "supplier_hotel_code", nullable = false, length = 50, updatable = false)
    private String supplierHotelCode;

    @Column(nullable = false)
    private String name;

    protected Accommodation() {
        // JPA
    }

    public Accommodation(SupplierCode supplierCode, String supplierHotelCode, String name) {
        this.supplierCode = supplierCode;
        this.supplierHotelCode = supplierHotelCode;
        this.name = name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public SupplierCode getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierHotelCode() {
        return supplierHotelCode;
    }

    public String getName() {
        return name;
    }
}
