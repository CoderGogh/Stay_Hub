package com.example.stay_hub.mapping;

import java.util.List;

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
 * 공급사 숙소 목록(①)을 자사 표준 숙소·객실 타입 매핑으로 동기화한다.
 * 재고/요금과 달리 정적 콘텐츠 성격이라 검색 핫패스가 아닌 별도 시점에 호출한다.
 */
@Service
public class CatalogSyncService {

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
        catalogPorts.forEach(this::syncSupplier);
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
