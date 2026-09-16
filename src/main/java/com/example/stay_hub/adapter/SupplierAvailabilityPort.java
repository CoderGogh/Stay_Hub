package com.example.stay_hub.adapter;

import java.time.LocalDate;
import java.util.List;

import com.example.stay_hub.domain.SupplierCode;

import reactor.core.publisher.Mono;

/**
 * 공급사 재고/요금(②) 조회 포트.
 * 검색 핫패스에서 다중 공급사 동시 호출 -> Mono 비동기 반환.
 * MVC + WebClient, 병렬 호출 구간만 리액티브.
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
