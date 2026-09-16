package com.example.stay_hub.api;

import java.util.List;

public record StaySearchResponse(
        List<StayOffer> results,
        // 일부 공급사 실패 시 노출 (부분 실패 허용)
        List<String> failedSuppliers
) {
}
