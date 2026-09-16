package com.example.stay_hub.adapter;

import java.time.LocalDate;
import java.util.List;

import com.example.stay_hub.domain.SupplierCode;

import reactor.core.publisher.Mono;

/**
 * 공급사 재고/요금(②) 조회 포트.
 * 검색 핫패스에서 여러 공급사를 동시 호출해야 하므로 Mono로 비동기 반환한다.
 * (MVC 위에서 WebClient를 쓰되, 병렬 호출 구간만 리액티브로 처리)
 */
public interface SupplierAvailabilityPort {

    SupplierCode supplierCode();

    Mono<List<SupplierRoomAvailability>> fetchAvailability(
            List<String> hotelCodes,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int children
    );
}
