package com.example.stay_hub.mapping;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.stay_hub.adapter.SupplierCatalogPort;
import com.example.stay_hub.adapter.SupplierHotelCatalog;
import com.example.stay_hub.adapter.SupplierHotelCatalog.SupplierRoomCatalog;
import com.example.stay_hub.domain.Accommodation;
import com.example.stay_hub.domain.AccommodationRepository;
import com.example.stay_hub.domain.RoomType;
import com.example.stay_hub.domain.RoomTypeRepository;
import com.example.stay_hub.domain.SupplierCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceTest {

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private SupplierCatalogPort catalogPort;

    @Test
    void 신규_숙소와_객실타입은_저장한다() {
        SupplierHotelCatalog catalog = new SupplierHotelCatalog(
                "A-2001", "Hanul Bay Hotel",
                List.of(new SupplierRoomCatalog("DLX-TWN", "Deluxe Twin", 2)));
        when(catalogPort.supplierCode()).thenReturn(SupplierCode.SUPPLIER_A);
        when(catalogPort.fetchCatalog()).thenReturn(List.of(catalog));
        when(accommodationRepository.findBySupplierCodeAndSupplierHotelCode(SupplierCode.SUPPLIER_A, "A-2001"))
                .thenReturn(Optional.empty());
        Accommodation savedAccommodation = new Accommodation(SupplierCode.SUPPLIER_A, "A-2001", "Hanul Bay Hotel");
        when(accommodationRepository.save(any(Accommodation.class))).thenReturn(savedAccommodation);
        when(roomTypeRepository.findByAccommodationAndSupplierRoomTypeCode(savedAccommodation, "DLX-TWN"))
                .thenReturn(Optional.empty());

        CatalogSyncService service = new CatalogSyncService(
                List.of(catalogPort), accommodationRepository, roomTypeRepository);
        service.syncAll();

        verify(accommodationRepository).save(any(Accommodation.class));
        verify(roomTypeRepository).save(any(RoomType.class));
    }

    @Test
    void 이미_매핑된_숙소는_다시_저장하지_않고_이름만_갱신한다() {
        Accommodation existing = new Accommodation(SupplierCode.SUPPLIER_A, "A-2001", "Old Name");
        SupplierHotelCatalog catalog = new SupplierHotelCatalog("A-2001", "New Name", List.of());
        when(catalogPort.supplierCode()).thenReturn(SupplierCode.SUPPLIER_A);
        when(catalogPort.fetchCatalog()).thenReturn(List.of(catalog));
        when(accommodationRepository.findBySupplierCodeAndSupplierHotelCode(SupplierCode.SUPPLIER_A, "A-2001"))
                .thenReturn(Optional.of(existing));

        CatalogSyncService service = new CatalogSyncService(
                List.of(catalogPort), accommodationRepository, roomTypeRepository);
        service.syncAll();

        assertThat(existing.getName()).isEqualTo("New Name");
        verify(accommodationRepository, never()).save(any());
    }
}
