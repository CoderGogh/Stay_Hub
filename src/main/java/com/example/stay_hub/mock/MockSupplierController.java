package com.example.stay_hub.mock;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Supplier A/B를 흉내내는 Mock 컨트롤러.
 * 'mock' 프로필에서만 기동 (server.port=9090, application-mock.properties).
 * 숙소 목록(①)은 정적 콘텐츠 성격이라 장애 모드를 걸지 않는다 (안내 문서 A.3 참고).
 */
@Profile("mock")
@RestController
public class MockSupplierController {

    @GetMapping(value = "/a/v1/hotels", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> hotelsA() {
        return ResponseEntity.ok(MockSupplierData.A_HOTELS);
    }

    @GetMapping(value = "/b/api/properties", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> propertiesB() {
        return ResponseEntity.ok(MockSupplierData.B_PROPERTIES);
    }
}
