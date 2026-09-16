package com.example.stay_hub.adapter.a;

import java.util.List;

/**
 * Supplier A 숙소 목록(①) 원본 응답. GET /a/v1/hotels
 */
public record SupplierAHotelsResponse(List<Item> items) {

    public record Item(String hotelCode, String hotelName, List<RoomType> roomTypes) {

        public record RoomType(String roomTypeCode, String roomTypeName, int maxOccupancy) {
        }
    }
}
