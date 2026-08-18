# Day1 주문 요약 API

SKALA "SpringAI 이해 및 활용" 과정 Day 1 메인 실습 — 3계층 구조 위에 Spring AI `ChatClient`를 얹어, 주문 하나를 한국어 한 문장으로 요약하는 API를 완성한다. 이번 실습의 핵심 질문은 "AI를 어떻게 부르는가"가 아니라 **"AI가 실패해도 API는 어떻게 살아남는가"** 다.

```
GET /lab1/orders/{orderId}/summary?userId={userId}
→ { "orderId": "12345", "summary": "주문번호 12345의 무선 이어폰은 현재 배송 중이며, 도착 예정일은 2026년 8월 20일입니다." }
```

## 목차

- [왜 이렇게 설계했나](#왜-이렇게-설계했나)
- [빠른 시작](#빠른-시작)
- [API](#api)
- [프로젝트 구조](#프로젝트-구조)
- [고정 데이터](#고정-데이터)
- [테스트](#테스트)
- [기술 스택](#기술-스택)
- [실습 결과 보고서](#실습-결과-보고서)

## 왜 이렇게 설계했나

| 설계 판단 | 이유 |
|---|---|
| **Controller는 `ChatClient`를 모른다** | AI 호출은 반드시 Service 계층에서만 일어난다. 나중에 모델 공급자를 바꾸거나 요약 로직을 손봐도 웹 계층은 전혀 건드릴 필요가 없다 (`src/main/java/com/skala/day1/web/`에 `ChatClient` import가 없다 — `grep -rn ChatClient src/main/java/com/skala/day1/web/`로 직접 확인 가능). |
| **temperature·maxTokens는 호출부가 아니라 빈에 고정** | `Lab1AiConfig`에서 `temperature(0.0)` · `maxTokens(120)`을 못 박는다. 호출부마다 값을 정하게 두면 누군가 기본값(0.7)으로 부르는 순간부터 요약이 매번 달라진다. |
| **사용자 입력을 프롬프트 문자열에 직접 이어 붙이지 않는다** | `{placeholder}` + `.param()` 바인딩만 쓴다. 문자열 이어붙이기는 값이 지시문처럼 해석될 위험이 있다. |
| **`finishReason=length`를 정상 응답으로 취급하지 않는다** | maxTokens 상한에 걸려 문장이 중간에서 끊긴 응답을 그대로 사용자에게 보내지 않고, 잘림이 감지되면 폴백으로 대체한다. |
| **AI 호출 실패가 API 전체 실패로 번지지 않는다** | 모델 호출만 `try/catch`로 감싸고, 실패하면 원본 주문 정보("상품명 · 상태")로 폴백해 200을 반환한다. 권한 확인(`OrderNotFoundException`)은 이 `try` 바깥에 있어 폴백에 삼켜지지 않는다. |
| **예외 응답에 스택트레이스를 노출하지 않는다** | `@RestControllerAdvice` 한 곳에서만 예외를 변환한다. 상세는 서버 로그에만 남기고, 사용자에게는 안전한 문구와 추적 ID(`traceId`)만 돌려준다. |
| **없는 주문과 남의 주문을 구분해서 알려주지 않는다** | `OrderRepository.findByIdAndOwnerId(orderId, ownerId)`가 두 경우 모두 빈 `Optional`을 반환해 동일한 404로 응답한다. 존재 여부를 알려주면 공격자가 유효한 주문번호를 추측하는 데 쓸 수 있다. |

## 빠른 시작

사전 준비: **JDK 21**. Gradle은 wrapper가 알아서 받아오므로 별도 설치가 필요 없다.

```bash
git clone https://github.com/wodnjs2020136144/day1-order-summary.git
cd day1-order-summary

export OPENAI_API_KEY="sk-..."   # 키는 환경변수로만 주입한다 — 소스·설정 파일에 직접 쓰지 않는다
./gradlew bootRun                # VS Code라면 F5 (Run and Debug)
```

기동되면 아래로 확인한다.

```bash
curl 'http://localhost:8080/lab1/orders/12345/summary?userId=user1'   # 200 — 본인 주문
curl 'http://localhost:8080/lab1/orders/99999/summary?userId=user1'   # 404 — 99999는 user2 소유
```

Swagger UI: <http://localhost:8080/swagger-ui/index.html>

키 없이 도는 자동 테스트만 실행하려면(모델을 실제로 부르지 않는 단위·웹 계층 테스트):

```bash
./gradlew test
```

> 이미 8080 포트를 쓰는 프로세스가 있다면 `SERVER_PORT=8091 ./gradlew bootRun`처럼 포트를 바꿔서 띄울 수 있다.

## API

### `GET /lab1/orders/{orderId}/summary`

| 파라미터 | 위치 | 설명 |
|---|---|---|
| `orderId` | path | 주문번호 (예: `12345`) |
| `userId` | query | 조회 주체 — 본인 주문만 조회할 수 있다 (예: `user1`) |

| 응답 | 상황 |
|---|---|
| `200` `{ "orderId", "summary" }` | 본인 소유 주문 — AI 요약 성공 시 실제 요약 문장, 모델 호출 실패 시 폴백 문구(`"상품명 · 상태"`) |
| `404` `{ "message", "traceId": null }` | 주문이 없거나 본인 소유가 아님 (두 경우를 구분하지 않는다) |
| `503` `{ "message", "traceId" }` | 그 밖의 예기치 못한 오류. `traceId`로 서버 로그에서 상세를 추적할 수 있다 |

## 프로젝트 구조

```
src/main/java/com/skala/day1/
├── LabApplication.java
├── domain/
│   ├── Order.java              # record(id, ownerId, item, status, eta) — 밖으로 나가지 않는 안쪽 모델
│   └── OrderStatus.java        # enum — 프롬프트에 넣을 한국어 표기를 함께 들고 다닌다
├── config/
│   └── Lab1AiConfig.java       # 요약 전용 ChatClient 빈 (temperature=0.0, maxTokens=120)
├── service/
│   ├── OrderRepository.java        # 인메모리 저장소 — 권한은 쿼리 조건 안에서 걸러진다
│   ├── OrderSummaryService.java    # 업무 흐름 + AI 호출 + finishReason 검사 + 폴백
│   └── OrderNotFoundException.java
└── web/
    ├── OrderSummaryController.java # ChatClient를 모른다
    ├── SummaryResponse.java
    ├── ErrorResponse.java
    └── Lab1ExceptionHandler.java   # 예외 → 응답 변환은 이 한 곳에서만
```

## 고정 데이터

DB 대신 인메모리 `Map`을 쓴다 — 이번 실습의 학습 지점은 AI 계층이지 저장소가 아니기 때문이다.

| orderId | ownerId | item | status | eta |
|---|---|---|---|---|
| 12345 | user1 | 무선 이어폰 | 배송중 | 2026-08-20 |
| 12346 | user1 | 기계식 키보드 | 결제완료 | 2026-08-22 |
| 12347 | user1 | USB-C 케이블 | 배송완료 | 2026-08-15 |
| 99999 | **user2** | 캠핑 의자 | 배송중 | 2026-08-21 |

## 테스트

| 종류 | 파일 | 키 필요 여부 | 검증 내용 |
|---|---|---|---|
| 웹 계층 (`@WebMvcTest`) | `OrderSummaryControllerTest` | 불필요 | 정상 200 / `OrderNotFoundException` → 404 |
| 서비스 계층 (Mockito) | `OrderSummaryServiceTest` | 불필요 | 없는·남의 주문이면 모델을 아예 호출하지 않음, 모델 호출 실패 시 폴백 문구로 200 |

```bash
./gradlew test
```

Swagger UI에서의 수동 검증(재현성, 실제 모델 응답 품질, 503 경로 등)은 [실습 결과 보고서](#실습-결과-보고서)에 캡처와 함께 정리돼 있다.

## 기술 스택

JDK 21 · Spring Boot 4.1.0 · spring-ai-bom 2.0.0 · spring-ai-starter-model-openai (`gpt-4o-mini`) · springdoc-openapi-starter-webmvc-ui 2.8.6

## 실습 결과 보고서

[`report/SpringAI_Day1_P345_P322.md`](report/SpringAI_Day1_P345_P322.md) — 완료 기준 8개 항목을 Swagger UI 실제 호출 캡처로 검증한 결과, 오늘 학습한 핵심 개념과 구현 코드의 매핑, 담당 파트 상세 설명을 담고 있다.
