package com.example.stay_hub.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통합 숙박 상품 검색 API.
 * 검색 조건: 날짜·인원뿐 (지역/키워드 필터는 비범위, §3.4).
 */
@RestController
@RequestMapping("/api/v1/stays")
public class StaySearchController {

    private final StaySearchService staySearchService;

    public StaySearchController(StaySearchService staySearchService) {
        this.staySearchService = staySearchService;
    }

    @GetMapping("/search")
    public StaySearchResponse search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int adults,
            @RequestParam(defaultValue = "0") int children) {
        return staySearchService.search(checkIn, checkOut, adults, children);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidRequest(IllegalArgumentException ex) {
        return ex.getMessage();
    }
}
