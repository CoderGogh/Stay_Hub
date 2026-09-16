package com.example.stay_hub.api;

import java.util.List;

/**
 * 통합 검색 응답.
 * failedSuppliers: 일부 공급사 조회가 실패했다면 그 사실을 드러낸다 (부분 실패 허용).
 */
public record StaySearchResponse(
        List<StayOffer> results,
        List<String> failedSuppliers
) {
}
