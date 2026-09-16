package com.example.stay_hub.adapter;

/**
 * 공급사 재고/요금 조회(②) 정규화 결과.
 *
 * 요금: 숙박 전체 기간 총액(세금 포함, gross)으로 통일.
 * - Supplier A: 날짜별 (nightlyRate + taxAmount) 합산
 * - Supplier B: 제공하는 totalPrice(gross) 그대로 사용
 * 재고: 요청 기간 전체 예약 가능 객실 수 (일자별 최소값).
 */
public record SupplierRoomAvailability(
        String hotelCode,
        String roomTypeCode,
        boolean breakfastIncluded,
        String currency,
        long totalPrice,
        int minRemainingRooms
) {
}
