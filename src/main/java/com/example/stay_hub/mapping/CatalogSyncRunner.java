package com.example.stay_hub.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 기동 시점에 숙소 목록 매핑을 1회 동기화한다.
 *
 * 호출 시점 판단: 숙소 목록은 자주 바뀌지 않는 정적 콘텐츠(안내 문서 3.2① 참고)이므로
 * 매 검색 요청마다 호출할 필요가 없다. 반대로 재고/요금처럼 검색 핫패스에 넣지도 않는다.
 * 가장 단순하면서도 흐름을 끊김 없이 보여줄 수 있는 "기동 시 1회"를 택했다.
 * 실제 운영에서는 숙소 수가 늘어날수록 주기적 스케줄링이나 별도 동기화 트리거가 필요하지만,
 * 이 과제 범위에서는 다루지 않는다 (관리자 기능은 비범위).
 *
 * mock 프로필(Mock 서버 자신)에서는 동작하지 않아야 하므로 제외한다.
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
            // 기동 시점에 Mock/공급사가 아직 떠있지 않을 수 있으므로 앱 부팅 자체는 막지 않는다
            log.warn("공급사 숙소 목록 매핑 동기화 실패 - 검색 시 매핑 없는 숙소는 제외됨", e);
        }
    }
}
