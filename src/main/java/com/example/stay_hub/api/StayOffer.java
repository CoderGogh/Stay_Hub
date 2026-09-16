package com.example.stay_hub.api;

/**
 * 통합 검색 결과 1건 (자사 표준 숙박 상품).
 * 요금: 숙박 전체 기간 총액(세금 포함, gross)으로 통일.
 * 예약 불가(availableRooms=0)도 그대로 노출 — 필터링은 비범위(§3.4).
 */
public record StayOffer(
        Long accommodationId,
        String accommodationName,
        Long roomTypeId,
        String roomTypeName,
        int maxOccupancy,
        int availableRooms,
        boolean breakfastIncluded,
        String currency,
        long totalPrice,
        String sourceSupplier
) {
}
