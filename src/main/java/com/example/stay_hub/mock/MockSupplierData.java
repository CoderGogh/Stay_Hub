package com.example.stay_hub.mock;

/**
 * Mock Supplier 고정 응답 데이터.
 * 채점 대상이 아니므로 뼈대 수준으로 최소 구성.
 */
final class MockSupplierData {

    static final String A_HOTELS = """
            {
              "items": [
                {
                  "hotelCode": "A-2001",
                  "hotelName": "Hanul Bay Hotel",
                  "roomTypes": [
                    { "roomTypeCode": "DLX-TWN", "roomTypeName": "Deluxe Twin", "maxOccupancy": 2 }
                  ]
                },
                {
                  "hotelCode": "A-2002",
                  "hotelName": "Bomun Lake Suites",
                  "roomTypes": [
                    { "roomTypeCode": "STD-DBL", "roomTypeName": "Standard Double", "maxOccupancy": 2 }
                  ]
                }
              ]
            }
            """;

    static final String A_AVAILABILITY = """
            {
              "items": [
                {
                  "hotelCode": "A-2001",
                  "hotelName": "Hanul Bay Hotel",
                  "roomTypeCode": "DLX-TWN",
                  "roomTypeName": "Deluxe Twin",
                  "maxOccupancy": 2,
                  "breakfastIncluded": false,
                  "currency": "KRW",
                  "dailyRates": [
                    { "date": "2026-09-01", "remainingRooms": 3, "nightlyRate": 120000, "taxAmount": 12000 },
                    { "date": "2026-09-02", "remainingRooms": 1, "nightlyRate": 150000, "taxAmount": 15000 },
                    { "date": "2026-09-03", "remainingRooms": 5, "nightlyRate": 120000, "taxAmount": 12000 }
                  ]
                },
                {
                  "hotelCode": "A-2002",
                  "hotelName": "Bomun Lake Suites",
                  "roomTypeCode": "STD-DBL",
                  "roomTypeName": "Standard Double",
                  "maxOccupancy": 2,
                  "breakfastIncluded": false,
                  "currency": "KRW",
                  "dailyRates": [
                    { "date": "2026-09-01", "remainingRooms": 2, "nightlyRate": 88000, "taxAmount": 8800 },
                    { "date": "2026-09-02", "remainingRooms": 0, "nightlyRate": 99000, "taxAmount": 9900 },
                    { "date": "2026-09-03", "remainingRooms": 4, "nightlyRate": 88000, "taxAmount": 8800 }
                  ]
                }
              ]
            }
            """;

    static final String A_ERROR = """
            { "error": "SERVICE_UNAVAILABLE", "message": "temporarily unavailable" }
            """;

    static final String B_PROPERTIES = """
            {
              "resultCode": "0000",
              "resultMessage": "SUCCESS",
              "data": {
                "items": [
                  {
                    "propertyId": "B-9001",
                    "propertyName": "Hanul Bay Hotel",
                    "rooms": [
                      { "roomId": "R-501", "roomName": "Deluxe Twin Room", "maxOccupancy": 2 }
                    ]
                  }
                ]
              }
            }
            """;

    static final String B_SEARCH = """
            {
              "resultCode": "0000",
              "resultMessage": "SUCCESS",
              "data": {
                "items": [
                  {
                    "propertyId": "B-9001",
                    "propertyName": "Hanul Bay Hotel",
                    "roomId": "R-501",
                    "roomName": "Deluxe Twin Room",
                    "maxOccupancy": 2,
                    "breakfastIncluded": true,
                    "currency": "KRW",
                    "totalPrice": 429000,
                    "taxIncluded": true,
                    "inventory": [
                      { "date": "2026-09-01", "remainingRooms": 3 },
                      { "date": "2026-09-02", "remainingRooms": 1 },
                      { "date": "2026-09-03", "remainingRooms": 5 }
                    ]
                  }
                ]
              }
            }
            """;

    // Supplier B는 장애 상황에서도 HTTP 200을 반환한다 (resultCode로만 실패를 알림)
    static final String B_ERROR = """
            { "resultCode": "E503", "resultMessage": "TEMPORARILY_UNAVAILABLE", "data": null }
            """;

    private MockSupplierData() {
    }
}
