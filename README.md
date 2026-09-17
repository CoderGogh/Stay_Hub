# Stay Hub

여러 외부 숙박 공급사(Supplier)의 상품을 하나의 표준 모델로 통합해 제공하는 연동 백엔드입니다.
공급사마다 API 스펙이 달라 같은 숙박 상품도 서로 다른 형태로 표현되는데, 이를 흡수해 고객에게는
항상 동일한 형태의 검색 결과를 보여주는 것이 이 프로젝트의 핵심입니다.

## 기술 스택

- Java 21, Spring Boot 4.1.1
- Gradle (Groovy DSL)
- Spring WebClient — 외부 공급사 연동 전용
- H2 (인메모리 RDB)
- springdoc-openapi, resilience4j (선택 구현 대비 포함)

## 빌드 및 실행 방법

### 요구 사항
- JDK 21 (Gradle toolchain으로 자동 지정됨)

### 빌드
```bash
./gradlew build
```

### 실행 — Mock Supplier 서버 (포트 9090)
실제 외부 서비스를 호출하지 않고, 공급사 API 스펙대로 동작하는 Mock 서버로 대체합니다.
본 애플리케이션과 포트 충돌을 피하기 위해 별도 프로필로 기동합니다.
```bash
./gradlew bootRun --args='--spring.profiles.active=mock'
```

### 실행 — 본 애플리케이션 (포트 8080)
Mock 서버가 떠 있는 상태에서 별도 터미널에 실행합니다.
```bash
./gradlew bootRun
```
기동 시 공급사 숙소 목록을 조회해 매핑을 동기화한 뒤, 아래 API로 검색할 수 있습니다.

```bash
curl "http://localhost:8080/api/v1/stays/search?checkIn=2026-09-01&checkOut=2026-09-04&adults=2&children=0"
```

### Mock 장애 상황 재현
```bash
curl -X POST "http://localhost:9090/control/a/mode?value=error"         # Supplier A 장애 (HTTP 503)
curl -X POST "http://localhost:9090/control/b/mode?value=error"         # Supplier B 장애 (HTTP 200 + resultCode)
curl -X POST "http://localhost:9090/control/a/mode?value=no-response"   # 무응답 (타임아웃 유도)
curl -X POST "http://localhost:9090/control/a/mode?value=normal"        # 정상으로 복구
```

### 실행 — Docker
`mock-supplier`(포트 9090)와 `app`(포트 8080) 두 컨테이너를 함께 띄웁니다.
`app`은 컨테이너 네트워크에서 `mock-supplier` 서비스명으로 접근하도록 환경 변수를
오버라이드해 구성되어 있습니다 (`docker-compose.yml` 참고).
```bash
docker compose up -d --build
curl "http://localhost:8080/api/v1/stays/search?checkIn=2026-09-01&checkOut=2026-09-04&adults=2&children=0"
docker compose down
```

## 패키지 구조

```
domain/    자사 표준 숙박 상품 모델 (Accommodation, RoomType), 매핑 저장
adapter/   공급사 연동 포트 + 정규화 DTO + 공급사별(a/b) WebClient 어댑터
mapping/   공급사 숙소 목록 → 자사 매핑 동기화
api/       통합 검색 API (컨트롤러/서비스/응답 DTO)
config/    WebClient 등 인프라 설정
mock/      Supplier A/B를 흉내내는 Mock 컨트롤러 (mock 프로필 전용)
```

## 설계 의사결정

### 1. 표준 숙박 상품 모델 — 무엇을 표준으로 삼았는가

두 공급사 모두 "숙소 > 객실 타입" 2단계 구조를 공유하므로, 이 구조를 그대로 자사 표준으로 채택했습니다.
- **Accommodation(숙소)**: `(공급사 코드, 공급사 숙소코드)`가 유일 — 이 조합 자체가 매핑 정보를 겸합니다.
- **RoomType(객실 타입)**: `(숙소, 공급사 객실타입코드)`가 유일 — 객실 타입 코드는 공급사 스펙상 해당 숙소 안에서만 유일하기 때문에, 숙소 단위로 유일성 범위를 맞췄습니다.

**무엇을 버렸는가**: 개별 물리 객실(101호, 102호 등)은 두 공급사 모두 노출하지 않으므로 다루지 않습니다.
지역/주소 정보도 두 공급사 모두 제공하지 않아 검색 조건에서 제외했습니다(요구사항 범위와도 일치).

**공급사가 다르면 내부 식별자도 다릅니다.** 같은 실제 숙소를 A와 B가 각각 등록해도(A의 `A-10023`과 B의 `B77120`처럼)
공통 키가 없어 프로그램적으로 동일 여부를 판단할 수 없습니다. 그래서 기본 동작은 "공급사별로 별도 레코드"이며,
중복 상품을 합치는 것은 선택 구현 범위로 남겨두고 구현하지 않았습니다. 필수 조건인 "같은 공급사 상품은 항상
같은 내부 식별자로 조회된다"는 DB 유일성 제약으로 보장됩니다.

**요금/재고는 DB에 저장하지 않습니다.** 원본이 외부에 있고 조회할 때마다 값이 달라지므로, 저장해봤자 금방
최신 값과 어긋나 버립니다. 그래서 검색 시점마다 매번 실시간으로 조회하는 쪽을 택했습니다. DB에는 오직
"매핑"만 저장해, 검색 요청이 들어왔을 때
어떤 숙소 코드를 어느 공급사에 물어봐야 하는지 알 수 있게 합니다.

### 2. 매핑 동기화 시점

숙소 목록(①) API는 정적 콘텐츠 성격이라 자주 바뀌지 않습니다. 반면 재고/요금(②)은 조회할 때마다 값이 다릅니다.
이 성격 차이를 반영해, **매핑 동기화는 애플리케이션 기동 시 1회만** 수행하고, 매 검색 요청마다 반복되는
경로(핫패스)에는 끼워 넣지 않았습니다.
(`CatalogSyncRunner`) 실제 서비스라면 숙소 수가 늘어날수록 주기적 스케줄러나 운영자가 트리거하는 별도
동기화 API가 필요하겠지만, 관리자 기능은 이 프로젝트의 비범위이므로 다루지 않았습니다.

### 3. Supplier 연동 어댑터 — 경계 분리

`SupplierCatalogPort`(①)와 `SupplierAvailabilityPort`(②) 두 인터페이스로 공급사 접근을 추상화했습니다.
각 공급사 어댑터(`SupplierAAdapter`, `SupplierBAdapter`)는 원본 응답 DTO를 받아 정규화된 DTO
(`SupplierHotelCatalog`, `SupplierRoomAvailability`)로 변환한 뒤 반환하며, 공급사별 필드명이나 구조가
`domain`/`api` 계층으로 새어나가지 않도록 여기서 완전히 경계를 끊습니다.

**신규 Supplier를 추가하려면**:
1. 원본 응답 DTO 정의 (`adapter/{supplier}/` 패키지)
2. `SupplierCatalogPort`, `SupplierAvailabilityPort`를 구현하는 어댑터 작성 — 응답을 정규화 DTO로 변환
3. `SupplierCode`에 식별자 추가, WebClient 빈/타임아웃/API 키를 설정에 추가

검색 서비스(`StaySearchService`)는 `List<SupplierAvailabilityPort>`를 스프링이 주입해주는 모든 구현체에
대해 동작하므로, 어댑터만 추가하면 서비스 코드 변경 없이 새 공급사가 검색에 포함됩니다.

### 4. 요금·재고 정규화 규칙

- **요금**: 숙박 전체 기간 총액(세금 포함, gross)으로 통일했습니다. Supplier B가 이미 이 형태로 제공하고,
  실제 고객에게 보여줄 최종 결제 금액과 가장 가깝다고 판단했습니다. Supplier A는 날짜별 `(nightlyRate + taxAmount)`를
  합산해 동일한 형태로 환산합니다.
- **재고**: "요청 기간 전체를 예약할 수 있는 객실 수"로 통일했고, 값은 일자별 잔여 객실 수의 **최소값**입니다.
  하루라도 재고가 0이면 그 기간 전체를 예약할 수 없기 때문입니다. 0이면 예약 불가로 간주합니다.
- **예약 불가 상품 노출**: 필터링하지 않고 `availableRooms: 0`으로 그대로 응답에 포함시켰습니다. 정렬/필터링은
  이 프로젝트의 비범위이며, 응답에서 굳이 숨기기보다 있는 그대로 보여주는 편이 더 투명하다고 판단했습니다.

### 5. 연동 견고성

- **타임아웃**: 연결 2초, 응답 3초로 설정했습니다(`application.properties`). 공급사 하나의 지연이 전체 검색을
  무한정 붙잡지 않도록, 사용자 체감상 허용 가능한 수준에서 짧게 잡았습니다.
- **병렬 호출**: MVC 컨트롤러 위에서 WebClient의 리액티브 특성만 검색 핫패스에 활용했습니다. 공급사(및 50개 단위
  배치)별로 `Mono`를 만들고 `Flux.merge`로 동시에 구독해, 순차 호출 대비 전체 응답 시간을 공급사 중 가장 느린
  하나의 시간으로 수렴시킵니다.
- **부분 실패 허용**: 각 공급사 호출에 `onErrorResume`을 걸어, 하나가 실패해도 스트림 전체가 죽지 않고 나머지
  결과로 응답합니다. 실패한 공급사는 응답의 `failedSuppliers` 필드로 드러납니다.
- **실패 판정 통일**: Supplier A는 4xx/5xx HTTP 상태 코드로, Supplier B는 항상 HTTP 200을 주고 본문
  `resultCode`로만 실패를 알립니다. 이 차이를 어댑터 내부에서 흡수해 둘 다 동일한 `SupplierCallException`으로
  변환하므로, 상위 계층(검색 서비스)은 공급사가 실패를 어떻게 표현하는지 몰라도 됩니다.

### 6. 숙소가 수천 개로 늘어난다면

현재는 매핑 조회(`findAll`)와 배치 분할이 메모리 위에서 이루어집니다. 숙소가 수천 개 단위가 되면:
- 검색 시마다 전체 숙소를 조회하지 않고, 향후 지역/조건 필터가 추가된다면 그 조건으로 먼저 후보를 좁혀야 합니다
  (지금은 날짜·인원만 검색 조건이라 이 문제가 아직 드러나지 않습니다).
- 공급사별 배치 호출 수가 `(숙소 수 / 50)`개로 늘어나므로, 병렬 호출 동시성에 상한(예: `Flux.merge(..., concurrency)`)을
  두어 공급사 쪽 순간 부하(Rate Limit, `429`)를 조절할 필요가 있습니다.
- 매핑 동기화도 전량 upsert 대신 변경분만 반영하는 방식(예: 마지막 동기화 이후 변경된 것만)을 고려해야 합니다.

## 선택 구현 여부

필수 구현(①~⑥)에 집중했고, 아래 선택 항목은 시간 관계상 구현하지 않았습니다.
- 재시도 정책 / 서킷 브레이커: `resilience4j` 의존성은 미리 추가해 두었으나 적용은 하지 않았습니다.
  타임아웃 + 부분 실패 허용만으로 이미 "연동 견고성 필수 네 가지"는 충족되어, 우선순위를 필수 흐름 검증에 두었습니다.
- 요금/재고 캐시, 정규화 실패 데이터 격리, 중복 상품 병합, 통화 처리, 예약 대행 흐름: 모두 핵심 검색 흐름과
  직접 관련이 적어 설계·구현하지 않았습니다.

## 비범위 (Out of scope)

인증/인가, 결제 연동, 관리자 기능, 프론트엔드, 실제 외부 API 연동, 지역/키워드 검색 필터, 정렬/페이징.
