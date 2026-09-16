package com.example.stay_hub.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 기동 시 숙소 목록 매핑 1회 동기화.
 *
 * 호출 시점: 기동 시 1회 — 숙소 목록은 정적 콘텐츠라 매 요청 불필요,
 * 재고/요금처럼 검색 핫패스에도 안 넣음. 흐름 끊김 없이 보여주는 최소 구성.
 * 확장 시 주기적 스케줄링/별도 트리거 필요하지만 관리자 기능은 비범위라 생략.
 *
 * mock 프로필(Mock 서버 자신)은 제외.
 */
@Profile("!mock")
@Component
public class CatalogSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSyncRunner.class);

    private final CatalogSyncService catalogSyncService;

    public CatalogSyncRunner(CatalogSyncService catalogSyncService) {
        this.catalogSyncService = catalogSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            catalogSyncService.syncAll();
            log.info("공급사 숙소 목록 매핑 동기화 완료");
        } catch (Exception e) {
            // Mock/공급사 미기동 가능성 — 앱 부팅은 막지 않음
            log.warn("공급사 숙소 목록 매핑 동기화 실패 - 검색 시 매핑 없는 숙소는 제외됨", e);
        }
    }
}
