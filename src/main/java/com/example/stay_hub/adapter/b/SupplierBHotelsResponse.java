package com.example.stay_hub.adapter.b;

import java.util.List;

/**
 * Supplier B 숙소 목록(①) 원본 응답. GET /b/api/properties
 * 실패도 HTTP 200으로 내려오므로 resultCode로 성공 여부를 판정해야 한다.
 */
public record SupplierBHotelsResponse(String resultCode, String resultMessage, Data data) {

    public record Data(List<Item> items) {
    }

    public record Item(String propertyId, String propertyName, List<Room> rooms) {

        public record Room(String roomId, String roomName, int maxOccupancy) {
        }
    }
}
