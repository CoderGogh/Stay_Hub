package com.example.stay_hub.adapter.b;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.stay_hub.adapter.SupplierAvailabilityPort;
import com.example.stay_hub.adapter.SupplierCallException;
import com.example.stay_hub.adapter.SupplierCatalogPort;
import com.example.stay_hub.adapter.SupplierHotelCatalog;
import com.example.stay_hub.adapter.SupplierRoomAvailability;
import com.example.stay_hub.domain.SupplierCode;

import reactor.core.publisher.Mono;

/**
 * Supplier B 연동 어댑터.
 * 실패 판정: HTTP 항상 200 -> 본문 resultCode로 판정.
 * A의 4xx/5xx와 동일하게 SupplierCallException으로 통일 (상위 계층은 공급사별 차이 몰라도 됨).
 */
@Component
public class SupplierBAdapter implements SupplierCatalogPort, SupplierAvailabilityPort {

    private static final String SUCCESS_CODE = "0000";
    private static final String API_KEY_HEADER = "X-Api-Key";

    private final WebClient webClient;
    private final String apiKey;

    public SupplierBAdapter(
            @Qualifier("supplierBWebClient") WebClient webClient,
            @Value("${supplier.b.api-key}") String apiKey) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    @Override
    public SupplierCode supplierCode() {
        return SupplierCode.SUPPLIER_B;
    }

    @Override
    public List<SupplierHotelCatalog> fetchCatalog() {
        SupplierBHotelsResponse response = webClient.get()
                .uri("/b/api/properties")
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .bodyToMono(SupplierBHotelsResponse.class)
                .block();

        requireSuccess(response.resultCode(), response.resultMessage());

        return response.data().items().stream()
                .map(item -> new SupplierHotelCatalog(
                        item.propertyId(),
                        item.propertyName(),
                        item.rooms().stream()
                                .map(room -> new SupplierHotelCatalog.SupplierRoomCatalog(
                                        room.roomId(), room.roomName(), room.maxOccupancy()))
                                .toList()))
                .toList();
    }

    @Override
    public Mono<List<SupplierRoomAvailability>> fetchAvailability(
            List<String> hotelCodes, LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/b/api/search")
                        .queryParam("propertyIds", String.join(",", hotelCodes))
                        .queryParam("checkIn", checkIn)
                        .queryParam("checkOut", checkOut)
                        .queryParam("adults", adults)
                        .queryParam("children", children)
                        .build())
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .bodyToMono(SupplierBAvailabilityResponse.class)
                .flatMap(response -> {
                    if (!SUCCESS_CODE.equals(response.resultCode())) {
                        return Mono.error(new SupplierCallException(
                                supplierCode(),
                                "Supplier B 호출 실패: resultCode=" + response.resultCode()
                                        + ", message=" + response.resultMessage()));
                    }
                    List<SupplierRoomAvailability> results = response.data().items().stream()
                            .map(this::toAvailability)
                            .toList();
                    return Mono.just(results);
                });
    }

    private SupplierRoomAvailability toAvailability(SupplierBAvailabilityResponse.Item item) {
        int minRemainingRooms = item.inventory().stream()
                .mapToInt(SupplierBAvailabilityResponse.Item.Inventory::remainingRooms)
                .min()
                .orElse(0);

        return new SupplierRoomAvailability(
                item.propertyId(),
                item.roomId(),
                item.breakfastIncluded(),
                item.currency(),
                item.totalPrice(),
                minRemainingRooms
        );
    }

    private void requireSuccess(String resultCode, String resultMessage) {
        if (!SUCCESS_CODE.equals(resultCode)) {
            throw new SupplierCallException(
                    supplierCode(),
                    "Supplier B 호출 실패: resultCode=" + resultCode + ", message=" + resultMessage);
        }
    }
}
