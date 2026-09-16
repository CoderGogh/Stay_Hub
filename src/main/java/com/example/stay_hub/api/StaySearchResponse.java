package com.example.stay_hub.api;

import java.util.List;

/**
 * 통합 검색 응답.
 * failedSuppliers: 일부 공급사 실패 시 노출 (부분 실패 허용).
 */
public record StaySearchResponse(
        List<StayOffer> results,
        List<String> failedSuppliers
) {
}
