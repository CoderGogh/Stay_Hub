package com.example.stay_hub.mapping;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.stay_hub.adapter.SupplierCatalogPort;
import com.example.stay_hub.adapter.SupplierHotelCatalog;
import com.example.stay_hub.domain.AccommodationRepository;
import com.example.stay_hub.domain.RoomTypeRepository;
import com.example.stay_hub.domain.SupplierCode;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 회귀 방지 테스트: 한 공급사의 카탈로그 조회가 실패해도 다른 공급사 동기화는 계속되어야 한다.
 * 과거 catalogPorts.forEach 순회 구현에서 이 조건이 깨졌던 버그를 재현해 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceIsolationTest {

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private SupplierCatalogPort failingPort;

    @Mock
    private SupplierCatalogPort workingPort;

    @Test
    void 한_공급사_카탈로그_조회가_실패해도_다른_공급사는_동기화되어야_한다() {
        when(failingPort.supplierCode()).thenReturn(SupplierCode.SUPPLIER_A);
        when(failingPort.fetchCatalog()).thenThrow(new RuntimeException("Supplier A 다운"));

        when(workingPort.supplierCode()).thenReturn(SupplierCode.SUPPLIER_B);
        SupplierHotelCatalog catalog = new SupplierHotelCatalog("B-9001", "Hanul Bay Hotel", List.of());
        when(workingPort.fetchCatalog()).thenReturn(List.of(catalog));
        when(accommodationRepository.findBySupplierCodeAndSupplierHotelCode(SupplierCode.SUPPLIER_B, "B-9001"))
                .thenReturn(Optional.empty());

        CatalogSyncService service = new CatalogSyncService(
                List.of(failingPort, workingPort), accommodationRepository, roomTypeRepository);

        service.syncAll();

        // Supplier A가 실패해도 Supplier B는 저장까지 이어져야 한다
        verify(accommodationRepository).save(org.mockito.ArgumentMatchers.any());
    }
}
