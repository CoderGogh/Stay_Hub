package com.example.stay_hub.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.stay_hub.adapter.SupplierAvailabilityPort;
import com.example.stay_hub.adapter.SupplierRoomAvailability;
import com.example.stay_hub.domain.Accommodation;
import com.example.stay_hub.domain.AccommodationRepository;
import com.example.stay_hub.domain.RoomType;
import com.example.stay_hub.domain.RoomTypeRepository;
import com.example.stay_hub.domain.SupplierCode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 통합 검색 서비스.
 * 흐름(§3.5): 보유 숙소를 공급사별로 묶음 → 공급사 병렬 조회 → 정규화/병합 → 반환
 */
@Service
public class StaySearchService {

    private static final Logger log = LoggerFactory.getLogger(StaySearchService.class);

    // 공급사 재고/요금 API는 한 번에 최대 50개 숙소 코드만 받는다 (부록 A.1/A.2)
    private static final int SUPPLIER_BATCH_SIZE = 50;

    private final AccommodationRepository accommodationRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final List<SupplierAvailabilityPort> availabilityPorts;

    public StaySearchService(
            AccommodationRepository accommodationRepository,
            RoomTypeRepository roomTypeRepository,
            List<SupplierAvailabilityPort> availabilityPorts) {
        this.accommodationRepository = accommodationRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.availabilityPorts = availabilityPorts;
    }

    public StaySearchResponse search(LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        if (adults < 1) {
            throw new IllegalArgumentException("adults must be at least 1");
        }
        if (children < 0) {
            throw new IllegalArgumentException("children must not be negative");
        }

        List<Accommodation> accommodations = accommodationRepository.findAll();
        Map<SupplierCode, List<Accommodation>> accommodationsBySupplier = accommodations.stream()
                .collect(Collectors.groupingBy(Accommodation::getSupplierCode));

        Map<String, RoomType> roomTypeLookup = buildRoomTypeLookup(accommodations);

        List<Mono<SupplierCallResult>> calls = buildCalls(
                accommodationsBySupplier, checkIn, checkOut, adults, children);

        // 병렬 호출: 공급사(및 배치)별 Mono 동시 구독, 전체 완료까지 대기
        List<SupplierCallResult> callResults = Flux.merge(calls).collectList().block();

        return merge(callResults, roomTypeLookup);
    }

    private List<Mono<SupplierCallResult>> buildCalls(
            Map<SupplierCode, List<Accommodation>> accommodationsBySupplier,
            LocalDate checkIn, LocalDate checkOut, int adults, int children) {

        List<Mono<SupplierCallResult>> calls = new ArrayList<>();
        for (SupplierAvailabilityPort port : availabilityPorts) {
            List<Accommodation> supplierAccommodations = accommodationsBySupplier
                    .getOrDefault(port.supplierCode(), List.of());
            if (supplierAccommodations.isEmpty()) {
                continue;
            }

            List<String> hotelCodes = supplierAccommodations.stream()
                    .map(Accommodation::getSupplierHotelCode)
                    .toList();

            for (List<String> batch : chunk(hotelCodes, SUPPLIER_BATCH_SIZE)) {
                calls.add(callSupplier(port, batch, checkIn, checkOut, adults, children));
            }
        }
        return calls;
    }

    private Mono<SupplierCallResult> callSupplier(
            SupplierAvailabilityPort port, List<String> hotelCodes,
            LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        return port.fetchAvailability(hotelCodes, checkIn, checkOut, adults, children)
                .map(items -> new SupplierCallResult(port.supplierCode(), items, true))
                // 부분 실패 허용: 예외를 실패 표시 결과로 흡수 (스트림 유지, 나머지로 응답)
                .onErrorResume(ex -> {
                    log.warn("공급사 조회 실패: supplier={}, hotelCodes={}", port.supplierCode(), hotelCodes, ex);
                    return Mono.just(new SupplierCallResult(port.supplierCode(), List.of(), false));
                });
    }

    private StaySearchResponse merge(List<SupplierCallResult> callResults, Map<String, RoomType> roomTypeLookup) {
        List<StayOffer> offers = new ArrayList<>();
        Set<SupplierCode> failedSuppliers = new LinkedHashSet<>();

        for (SupplierCallResult result : callResults) {
            if (!result.success()) {
                failedSuppliers.add(result.supplierCode());
                continue;
            }
            for (SupplierRoomAvailability availability : result.items()) {
                RoomType roomType = roomTypeLookup.get(lookupKey(
                        result.supplierCode(), availability.hotelCode(), availability.roomTypeCode()));
                if (roomType == null) {
                    // 매핑 없는 상품(동기화 전/누락) — 내부 식별자 없어 노출 불가
                    log.warn("매핑되지 않은 공급사 상품 - 검색 결과에서 제외: supplier={}, hotelCode={}, roomTypeCode={}",
                            result.supplierCode(), availability.hotelCode(), availability.roomTypeCode());
                    continue;
                }
                offers.add(toOffer(roomType, availability, result.supplierCode()));
            }
        }

        return new StaySearchResponse(
                offers,
                failedSuppliers.stream().map(Enum::name).toList());
    }

    private StayOffer toOffer(RoomType roomType, SupplierRoomAvailability availability, SupplierCode supplierCode) {
        Accommodation accommodation = roomType.getAccommodation();
        return new StayOffer(
                accommodation.getId(),
                accommodation.getName(),
                roomType.getId(),
                roomType.getName(),
                roomType.getMaxOccupancy(),
                availability.minRemainingRooms(),
                availability.breakfastIncluded(),
                availability.currency(),
                availability.totalPrice(),
                supplierCode.name());
    }

    private Map<String, RoomType> buildRoomTypeLookup(List<Accommodation> accommodations) {
        List<RoomType> roomTypes = roomTypeRepository.findByAccommodationIn(accommodations);
        Map<String, RoomType> lookup = new HashMap<>();
        for (RoomType roomType : roomTypes) {
            Accommodation accommodation = roomType.getAccommodation();
            lookup.put(lookupKey(
                    accommodation.getSupplierCode(),
                    accommodation.getSupplierHotelCode(),
                    roomType.getSupplierRoomTypeCode()), roomType);
        }
        return lookup;
    }

    private static String lookupKey(SupplierCode supplierCode, String hotelCode, String roomTypeCode) {
        return supplierCode + "|" + hotelCode + "|" + roomTypeCode;
    }

    private static List<List<String>> chunk(List<String> values, int size) {
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < values.size(); i += size) {
            chunks.add(values.subList(i, Math.min(i + size, values.size())));
        }
        return chunks;
    }

    private record SupplierCallResult(SupplierCode supplierCode, List<SupplierRoomAvailability> items, boolean success) {
    }
}
