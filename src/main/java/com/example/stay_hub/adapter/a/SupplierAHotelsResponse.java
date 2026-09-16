package com.example.stay_hub.adapter.a;

import java.util.List;

// GET /a/v1/hotels 원본 응답
public record SupplierAHotelsResponse(List<Item> items) {

    public record Item(String hotelCode, String hotelName, List<RoomType> roomTypes) {

        public record RoomType(String roomTypeCode, String roomTypeName, int maxOccupancy) {
        }
    }
}
