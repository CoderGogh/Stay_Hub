package com.example.stay_hub.adapter.b;

import java.time.LocalDate;
import java.util.List;

/**
 * Supplier B 재고/요금 조회(②) 원본 응답. GET /b/api/search
 * totalPrice는 세금 포함(gross) 총액. 날짜별 요금은 제공하지 않는다.
 */
public record SupplierBAvailabilityResponse(String resultCode, String resultMessage, Data data) {

    public record Data(List<Item> items) {
    }

    public record Item(
            String propertyId,
            String propertyName,
            String roomId,
            String roomName,
            int maxOccupancy,
            boolean breakfastIncluded,
            String currency,
            long totalPrice,
            boolean taxIncluded,
            List<Inventory> inventory
    ) {

        public record Inventory(LocalDate date, int remainingRooms) {
        }
    }
}
