package com.example.stay_hub.mapping;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stay_hub.adapter.SupplierCatalogPort;
import com.example.stay_hub.adapter.SupplierHotelCatalog;
import com.example.stay_hub.domain.Accommodation;
import com.example.stay_hub.domain.AccommodationRepository;
import com.example.stay_hub.domain.RoomType;
import com.example.stay_hub.domain.RoomTypeRepository;
import com.example.stay_hub.domain.SupplierCode;

/**
 * 공급사 숙소 목록(①) -> 자사 표준 숙소·객실 타입 매핑 동기화.
 * 정적 콘텐츠 성격 — 검색 핫패스 아닌 별도 시점에 호출.
 */
@Service
public class CatalogSyncService {

    private static final Logger log = LoggerFactory.getLogger(CatalogSyncService.class);

    private final List<SupplierCatalogPort> catalogPorts;
    private final AccommodationRepository accommodationRepository;
    private final RoomTypeRepository roomTypeRepository;

    public CatalogSyncService(
            List<SupplierCatalogPort> catalogPorts,
            AccommodationRepository accommodationRepository,
            RoomTypeRepository roomTypeRepository) {
        this.catalogPorts = catalogPorts;
        this.accommodationRepository = accommodationRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional
    public void syncAll() {
        // 공급사별 실패 격리 — 검색 흐름의 부분 실패 허용과 동일 원칙
        for (SupplierCatalogPort port : catalogPorts) {
            try {
                syncSupplier(port);
            } catch (Exception e) {
                log.warn("공급사 카탈로그 동기화 실패 - 다른 공급사는 계속 진행: supplier={}", port.supplierCode(), e);
            }
        }
    }

    private void syncSupplier(SupplierCatalogPort port) {
        List<SupplierHotelCatalog> catalog = port.fetchCatalog();
        catalog.forEach(hotel -> syncHotel(port.supplierCode(), hotel));
    }

    private void syncHotel(SupplierCode supplierCode, SupplierHotelCatalog hotel) {
        Accommodation accommodation = accommodationRepository
                .findBySupplierCodeAndSupplierHotelCode(supplierCode, hotel.hotelCode())
                .map(existing -> {
                    existing.rename(hotel.hotelName());
                    return existing;
                })
                .orElseGet(() -> accommodationRepository.save(
                        new Accommodation(supplierCode, hotel.hotelCode(), hotel.hotelName())));

        hotel.roomTypes().forEach(room -> syncRoomType(accommodation, room));
    }

    private void syncRoomType(Accommodation accommodation, SupplierHotelCatalog.SupplierRoomCatalog room) {
        roomTypeRepository.findByAccommodationAndSupplierRoomTypeCode(accommodation, room.roomTypeCode())
                .ifPresentOrElse(
                        existing -> existing.update(room.roomTypeName(), room.maxOccupancy()),
                        () -> roomTypeRepository.save(new RoomType(
                                accommodation, room.roomTypeCode(), room.roomTypeName(), room.maxOccupancy()))
                );
    }
}
