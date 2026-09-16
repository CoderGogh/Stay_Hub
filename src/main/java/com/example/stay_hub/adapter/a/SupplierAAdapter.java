package com.example.stay_hub.adapter.a;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.stay_hub.adapter.SupplierAvailabilityPort;
import com.example.stay_hub.adapter.SupplierCallException;
import com.example.stay_hub.adapter.SupplierCatalogPort;
import com.example.stay_hub.adapter.SupplierHotelCatalog;
import com.example.stay_hub.adapter.SupplierRoomAvailability;
import com.example.stay_hub.domain.SupplierCode;

import reactor.core.publisher.Mono;

/**
 * Supplier A 연동 어댑터.
 * 실패 판정: HTTP 상태 코드 기반 — 4xx/5xx를 SupplierCallException으로 통일.
 */
@Component
public class SupplierAAdapter implements SupplierCatalogPort, SupplierAvailabilityPort {

    private static final String API_KEY_HEADER = "X-Api-Key";

    private final WebClient webClient;
    private final String apiKey;

    public SupplierAAdapter(
            @Qualifier("supplierAWebClient") WebClient webClient,
            @Value("${supplier.a.api-key}") String apiKey) {
        this.webClient = webClient;
        this.apiKey = apiKey;
    }

    @Override
    public SupplierCode supplierCode() {
        return SupplierCode.SUPPLIER_A;
    }

    @Override
    public List<SupplierHotelCatalog> fetchCatalog() {
        SupplierAHotelsResponse response = webClient.get()
                .uri("/a/v1/hotels")
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toCallException)
                .bodyToMono(SupplierAHotelsResponse.class)
                .block();

        return response.items().stream()
                .map(item -> new SupplierHotelCatalog(
                        item.hotelCode(),
                        item.hotelName(),
                        item.roomTypes().stream()
                                .map(rt -> new SupplierHotelCatalog.SupplierRoomCatalog(
                                        rt.roomTypeCode(), rt.roomTypeName(), rt.maxOccupancy()))
                                .toList()))
                .toList();
    }

    @Override
    public Mono<List<SupplierRoomAvailability>> fetchAvailability(
            List<String> hotelCodes, LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/a/v1/availability")
                        .queryParam("hotelCodes", String.join(",", hotelCodes))
                        .queryParam("checkIn", checkIn)
                        .queryParam("checkOut", checkOut)
                        .queryParam("adults", adults)
                        .queryParam("children", children)
                        .build())
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toCallException)
                .bodyToMono(SupplierAAvailabilityResponse.class)
                .map(response -> response.items().stream()
                        .map(this::toAvailability)
                        .toList());
    }

    private SupplierRoomAvailability toAvailability(SupplierAAvailabilityResponse.Item item) {
        // 세금 별도(net) -> gross 총액으로 환산 (일자별 nightlyRate+taxAmount 합산)
        long totalPrice = item.dailyRates().stream()
                .mapToLong(rate -> rate.nightlyRate() + rate.taxAmount())
                .sum();
        // 요청 기간 전체 예약 가능 객실 수 = 일자별 잔여 객실 수 최소값
        int minRemainingRooms = item.dailyRates().stream()
                .mapToInt(SupplierAAvailabilityResponse.Item.DailyRate::remainingRooms)
                .min()
                .orElse(0);

        return new SupplierRoomAvailability(
                item.hotelCode(),
                item.roomTypeCode(),
                item.breakfastIncluded(),
                item.currency(),
                totalPrice,
                minRemainingRooms
        );
    }

    private Mono<? extends Throwable> toCallException(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new SupplierCallException(
                        supplierCode(),
                        "Supplier A 호출 실패: status=" + response.statusCode() + ", body=" + body));
    }
}
