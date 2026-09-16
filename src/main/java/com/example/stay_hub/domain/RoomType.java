package com.example.stay_hub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 자사 표준 객실 타입.
 * 객실 타입 코드는 "해당 숙소 안에서만" 유일 (공급사 스펙) -> 유일성 제약도 숙소 단위로.
 */
@Entity
@Table(
        name = "room_type",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_type_accommodation_code",
                columnNames = {"accommodation_id", "supplier_room_type_code"}
        )
)
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false, updatable = false)
    private Accommodation accommodation;

    // 공급사 쪽 객실 타입 식별자 (Supplier A: roomTypeCode / Supplier B: roomId)
    @Column(name = "supplier_room_type_code", nullable = false, length = 50, updatable = false)
    private String supplierRoomTypeCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "max_occupancy", nullable = false)
    private int maxOccupancy;

    protected RoomType() {
        // JPA
    }

    public RoomType(Accommodation accommodation, String supplierRoomTypeCode, String name, int maxOccupancy) {
        this.accommodation = accommodation;
        this.supplierRoomTypeCode = supplierRoomTypeCode;
        this.name = name;
        this.maxOccupancy = maxOccupancy;
    }

    public void update(String name, int maxOccupancy) {
        this.name = name;
        this.maxOccupancy = maxOccupancy;
    }

    public Long getId() {
        return id;
    }

    public Accommodation getAccommodation() {
        return accommodation;
    }

    public String getSupplierRoomTypeCode() {
        return supplierRoomTypeCode;
    }

    public String getName() {
        return name;
    }

    public int getMaxOccupancy() {
        return maxOccupancy;
    }
}
