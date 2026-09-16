package com.example.stay_hub.adapter;

import java.util.List;

/**
 * 공급사 숙소 목록(①) 정규화 결과.
 * 정적 콘텐츠 성격 — 요금/재고를 포함하지 않는다.
 */
public record SupplierHotelCatalog(
        String hotelCode,
        String hotelName,
        List<SupplierRoomCatalog> roomTypes
) {

    public record SupplierRoomCatalog(
            String roomTypeCode,
            String roomTypeName,
            int maxOccupancy
    ) {
    }
}
