package com.example.stay_hub.adapter.a;

import java.time.LocalDate;
import java.util.List;

/**
 * Supplier A 재고/요금 조회(②) 원본 응답. GET /a/v1/availability
 * 요금은 세금 별도(net) — nightlyRate + taxAmount가 해당 날짜 고객 결제 금액.
 */
public record SupplierAAvailabilityResponse(List<Item> items) {

    public record Item(
            String hotelCode,
            String hotelName,
            String roomTypeCode,
            String roomTypeName,
            int maxOccupancy,
            boolean breakfastIncluded,
            String currency,
            List<DailyRate> dailyRates
    ) {

        public record DailyRate(LocalDate date, int remainingRooms, long nightlyRate, long taxAmount) {
        }
    }
}
