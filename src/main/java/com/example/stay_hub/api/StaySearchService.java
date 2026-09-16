package com.example.stay_hub.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.stay_hub.domain.Accommodation;
import com.example.stay_hub.domain.AccommodationRepository;
import com.example.stay_hub.domain.RoomType;
import com.example.stay_hub.domain.RoomTypeRepository;
import com.example.stay_hub.domain.SupplierCode;

/**
 * 통합 검색 서비스.
 * 흐름(§3.5): 보유 숙소를 공급사별로 묶음 → 공급사 병렬 조회 → 정규화/병합 → 반환
 */
@Service
public class StaySearchService {

    // 공급사 재고/요금 API는 한 번에 최대 50개 숙소 코드만 받는다 (부록 A.1/A.2)
    private static final int SUPPLIER_BATCH_SIZE = 50;

    private final AccommodationRepository accommodationRepository;
    private final RoomTypeRepository roomTypeRepository;

    public StaySearchService(
            AccommodationRepository accommodationRepository,
            RoomTypeRepository roomTypeRepository) {
        this.accommodationRepository = accommodationRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    public StaySearchResponse search(LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }

        List<Accommodation> accommodations = accommodationRepository.findAll();
        Map<SupplierCode, List<Accommodation>> accommodationsBySupplier = accommodations.stream()
                .collect(Collectors.groupingBy(Accommodation::getSupplierCode));

        Map<String, RoomType> roomTypeLookup = buildRoomTypeLookup(accommodations);

        return new StaySearchResponse(List.of(), List.of());
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
}
