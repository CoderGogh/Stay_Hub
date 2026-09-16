package com.example.stay_hub.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.stay_hub.adapter.SupplierAvailabilityPort;
import com.example.stay_hub.adapter.SupplierCallException;
import com.example.stay_hub.adapter.SupplierRoomAvailability;
import com.example.stay_hub.domain.Accommodation;
import com.example.stay_hub.domain.AccommodationRepository;
import com.example.stay_hub.domain.RoomType;
import com.example.stay_hub.domain.RoomTypeRepository;
import com.example.stay_hub.domain.SupplierCode;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaySearchServiceTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 9, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 9, 4);

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private SupplierAvailabilityPort portA;

    @Mock
    private SupplierAvailabilityPort portB;

    @Test
    void 두_공급사_결과를_정규화해서_병합한다() {
        Accommodation accA = new Accommodation(SupplierCode.SUPPLIER_A, "A-2001", "Hanul Bay Hotel");
        Accommodation accB = new Accommodation(SupplierCode.SUPPLIER_B, "B-9001", "Hanul Bay Hotel");
        RoomType rtA = new RoomType(accA, "DLX-TWN", "Deluxe Twin", 2);
        RoomType rtB = new RoomType(accB, "R-501", "Deluxe Twin Room", 2);
        List<Accommodation> accommodations = List.of(accA, accB);
        when(accommodationRepository.findAll()).thenReturn(accommodations);
        when(roomTypeRepository.findByAccommodationIn(accommodations)).thenReturn(List.of(rtA, rtB));
        lenient().when(portA.supplierCode()).thenReturn(SupplierCode.SUPPLIER_A);
        lenient().when(portB.supplierCode()).thenReturn(SupplierCode.SUPPLIER_B);
        when(portA.fetchAvailability(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Mono.just(List.of(
                        new SupplierRoomAvailability("A-2001", "DLX-TWN", false, "KRW", 429_000L, 1))));
        when(portB.fetchAvailability(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Mono.just(List.of(
                        new SupplierRoomAvailability("B-9001", "R-501", true, "KRW", 452_000L, 3))));

        StaySearchService service = new StaySearchService(
                accommodationRepository, roomTypeRepository, List.of(portA, portB));
        StaySearchResponse response = service.search(CHECK_IN, CHECK_OUT, 2, 0);

        assertThat(response.failedSuppliers()).isEmpty();
        assertThat(response.results()).hasSize(2);
        assertThat(response.results())
                .extracting(StayOffer::sourceSupplier, StayOffer::totalPrice, StayOffer::availableRooms)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("SUPPLIER_A", 429_000L, 1),
                        org.assertj.core.groups.Tuple.tuple("SUPPLIER_B", 452_000L, 3));
    }

    @Test
    void 한_공급사가_실패해도_나머지_결과로_응답하고_실패_사실을_노출한다() {
        Accommodation accA = new Accommodation(SupplierCode.SUPPLIER_A, "A-2001", "Hanul Bay Hotel");
        Accommodation accB = new Accommodation(SupplierCode.SUPPLIER_B, "B-9001", "Hanul Bay Hotel");
        RoomType rtA = new RoomType(accA, "DLX-TWN", "Deluxe Twin", 2);
        List<Accommodation> accommodations = List.of(accA, accB);
        when(accommodationRepository.findAll()).thenReturn(accommodations);
        when(roomTypeRepository.findByAccommodationIn(accommodations)).thenReturn(List.of(rtA));
        lenient().when(portA.supplierCode()).thenReturn(SupplierCode.SUPPLIER_A);
        lenient().when(portB.supplierCode()).thenReturn(SupplierCode.SUPPLIER_B);
        when(portA.fetchAvailability(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Mono.just(List.of(
                        new SupplierRoomAvailability("A-2001", "DLX-TWN", false, "KRW", 429_000L, 1))));
        when(portB.fetchAvailability(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Mono.error(new SupplierCallException(SupplierCode.SUPPLIER_B, "장애 상황 재현")));

        StaySearchService service = new StaySearchService(
                accommodationRepository, roomTypeRepository, List.of(portA, portB));
        StaySearchResponse response = service.search(CHECK_IN, CHECK_OUT, 2, 0);

        assertThat(response.failedSuppliers()).containsExactly("SUPPLIER_B");
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).sourceSupplier()).isEqualTo("SUPPLIER_A");
    }

    @Test
    void 숙소가_50개를_넘으면_배치로_나누어_호출한다() {
        List<Accommodation> accommodations = new ArrayList<>();
        for (int i = 1; i <= 51; i++) {
            accommodations.add(new Accommodation(SupplierCode.SUPPLIER_A, "A-" + i, "Hotel " + i));
        }
        when(accommodationRepository.findAll()).thenReturn(accommodations);
        when(roomTypeRepository.findByAccommodationIn(accommodations)).thenReturn(List.of());
        lenient().when(portA.supplierCode()).thenReturn(SupplierCode.SUPPLIER_A);
        lenient().when(portB.supplierCode()).thenReturn(SupplierCode.SUPPLIER_B);
        when(portA.fetchAvailability(anyList(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Mono.just(List.of()));

        StaySearchService service = new StaySearchService(
                accommodationRepository, roomTypeRepository, List.of(portA, portB));
        service.search(CHECK_IN, CHECK_OUT, 2, 0);

        ArgumentCaptor<List<String>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(portA, times(2)).fetchAvailability(batchCaptor.capture(), any(), any(), anyInt(), anyInt());
        assertThat(batchCaptor.getAllValues())
                .extracting(List::size)
                .containsExactlyInAnyOrder(50, 1);
    }

    @Test
    void 체크아웃이_체크인보다_뒤가_아니면_예외() {
        StaySearchService service = new StaySearchService(
                accommodationRepository, roomTypeRepository, List.of(portA, portB));

        assertThatThrownBy(() -> service.search(CHECK_IN, CHECK_IN, 2, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 성인_인원이_1명_미만이면_예외() {
        StaySearchService service = new StaySearchService(
                accommodationRepository, roomTypeRepository, List.of(portA, portB));

        assertThatThrownBy(() -> service.search(CHECK_IN, CHECK_OUT, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(CHECK_IN, CHECK_OUT, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 아동_인원이_음수이면_예외() {
        StaySearchService service = new StaySearchService(
                accommodationRepository, roomTypeRepository, List.of(portA, portB));

        assertThatThrownBy(() -> service.search(CHECK_IN, CHECK_OUT, 2, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
