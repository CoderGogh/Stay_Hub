package com.example.stay_hub.api;

/**
 * 통합 검색 결과 1건 (자사 표준 숙박 상품).
 * 요금은 숙박 전체 기간 총액(세금 포함, gross)으로 통일해 제공한다.
 * 예약 불가 상품(availableRooms=0)도 그대로 노출한다 — 필터링은 이 과제 범위(§3.4)가 아님.
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
