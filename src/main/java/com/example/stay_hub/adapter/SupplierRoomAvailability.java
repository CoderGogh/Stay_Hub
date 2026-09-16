package com.example.stay_hub.adapter;

/**
 * 공급사 재고/요금 조회(②) 정규화 결과.
 *
 * 요금은 '숙박 전체 기간 총액(세금 포함, gross)' 하나로 통일한다.
 * - Supplier A: 날짜별 (nightlyRate + taxAmount)를 합산해 총액으로 환산
 * - Supplier B: 이미 제공하는 totalPrice(gross)를 그대로 사용
 * 재고는 '요청 기간 전체를 예약 가능한 객실 수'로 통일한다 (일자별 최소값).
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
