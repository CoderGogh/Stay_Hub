package com.example.stay_hub.adapter;

import java.util.List;

import com.example.stay_hub.domain.SupplierCode;

/**
 * 공급사 숙소 목록(①) 조회 포트.
 * 정적 콘텐츠 -> 카탈로그 동기화 시점에만 호출 (검색 핫패스 제외).
 */
public interface SupplierCatalogPort {

    SupplierCode supplierCode();

    List<SupplierHotelCatalog> fetchCatalog();
}
