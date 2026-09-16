package com.example.stay_hub.mock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Supplier A/B를 흉내내는 Mock 컨트롤러.
 * 'mock' 프로필에서만 기동 (server.port=9090, application-mock.properties).
 * 숙소 목록(①)은 정적 콘텐츠 성격이라 장애 모드를 걸지 않는다 (안내 문서 A.3 참고).
 */
@Profile("mock")
@RestController
public class MockSupplierController {

    private static final String MODE_NORMAL = "normal";
    private static final String MODE_ERROR = "error";
    private static final String MODE_NO_RESPONSE = "no-response";
    private static final long NO_RESPONSE_DELAY_MS = 600_000L;

    // 공급사별 장애 모드 (a|b -> normal|error|no-response)
    private final Map<String, String> modes = new ConcurrentHashMap<>();

    @GetMapping(value = "/a/v1/hotels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> hotelsA() {
        return ResponseEntity.ok(MockSupplierData.A_HOTELS);
    }

    @GetMapping(value = "/b/api/properties", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> propertiesB() {
        return ResponseEntity.ok(MockSupplierData.B_PROPERTIES);
    }

    @GetMapping(value = "/a/v1/availability", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> availabilityA(@RequestParam String hotelCodes) throws InterruptedException {
        return switch (modeOf("a")) {
            case MODE_ERROR -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(MockSupplierData.A_ERROR);
            case MODE_NO_RESPONSE -> {
                Thread.sleep(NO_RESPONSE_DELAY_MS);
                yield ResponseEntity.ok("{}");
            }
            default -> ResponseEntity.ok(MockSupplierData.A_AVAILABILITY);
        };
    }

    @GetMapping(value = "/b/api/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchB(@RequestParam String propertyIds) throws InterruptedException {
        return switch (modeOf("b")) {
            // Supplier B는 장애 상황에서도 HTTP 200을 반환
            case MODE_ERROR -> ResponseEntity.ok(MockSupplierData.B_ERROR);
            case MODE_NO_RESPONSE -> {
                Thread.sleep(NO_RESPONSE_DELAY_MS);
                yield ResponseEntity.ok("{}");
            }
            default -> ResponseEntity.ok(MockSupplierData.B_SEARCH);
        };
    }

    @PostMapping("/control/{supplier}/mode")
    public ResponseEntity<String> setMode(@PathVariable String supplier, @RequestParam String value) {
        modes.put(supplier, value);
        return ResponseEntity.ok("{\"supplier\":\"" + supplier + "\",\"mode\":\"" + value + "\"}");
    }

    private String modeOf(String supplier) {
        return modes.getOrDefault(supplier, MODE_NORMAL);
    }
}
