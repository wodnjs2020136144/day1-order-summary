# SpringAI 이해 및 활용 — Day 1 실습과제 결과 보고서

**메인 실습 · 주문 요약 API (Spring AI ChatClient + 3계층 + 폴백)**

| 항목        | 내용                                                                                                                                                             |
| ----------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 조원        | 황재원 (P345) · 박성우 (P322)                                                                                                                                    |
| 실습 일자   | 2026-08-18                                                                                                                                                       |
| 산출물 위치 | `SpringAI_실습/07_주문요약_메인실습` (별도 리포지토리: [github.com/wodnjs2020136144/day1-order-summary](https://github.com/wodnjs2020136144/day1-order-summary)) |
| 제출 성격   | 평가 없음 — 실행/테스트 결과 캡처 및 설명 제출                                                                                                                   |

---

## 1. 실습 개요

교안 메인 실습(p.94–101) 목표는 주문 하나를 AI가 한 문장으로 요약하는 API를 3계층 규칙대로 완성하는 것이다. 2인 1조로 계층 경계를 따라 역할을 나누어 직접 구현했다.

| 담당                                  | 조원          | 구현 파일                                                                          |
| ------------------------------------- | ------------- | ---------------------------------------------------------------------------------- |
| AI 계층 (설정·업무 흐름·폴백)         | 황재원 (P345) | `Lab1AiConfig.java`, `OrderSummaryService.java`                                    |
| 웹·데이터 계층 (저장소·컨트롤러·예외) | 박성우 (P322) | `OrderRepository.java`, `OrderSummaryController.java`, `Lab1ExceptionHandler.java` |

| 항목            | 내용                                                                                                |
| --------------- | --------------------------------------------------------------------------------------------------- |
| 엔드포인트      | `GET /lab1/orders/{orderId}/summary?userId=...`                                                     |
| 계층 구조       | Controller → Service → (Repository + ChatClient)                                                    |
| 사용 스택       | JDK 21 · Spring Boot 4.1.0 · spring-ai-bom 2.0.0 · spring-ai-starter-model-openai · springdoc 2.8.6 |
| ChatClient 옵션 | temperature = 0.0 · maxTokens = 120 (재현성·비용 상한 고정)                                         |
| 폴백 정책       | 모델 호출 실패 또는 `finishReason=length` 시 "상품명 · 상태" 형식으로 200 응답                      |

---

## 2. 자동 테스트 실행 결과

모델 호출 없이 도는 단위/웹 계층 테스트 2종을 실제로 실행해 통과를 확인했다 (`OrderSummaryControllerTest` 2건, `OrderSummaryServiceTest` 2건 — 총 4건 성공).

```
$ ./gradlew test

> Task :compileJava UP-TO-DATE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestJava
> Task :processTestResources NO-SOURCE
> Task :testClasses
> Task :test

BUILD SUCCESSFUL in 2s
4 actionable tasks: 2 executed, 2 up-to-date

[통과] OrderSummaryControllerTest
  - 정상_주문은_200과_요약을_돌려준다()
  - 남의_주문이거나_없는_주문은_404다()
[통과] OrderSummaryServiceTest
  - 없는_주문이거나_남의_주문이면_모델을_부르지_않고_예외를_던진다()
  - 모델_호출이_실패해도_주문_정보는_200으로_나간다()
```

**완료 기준 3번(계층 분리) 기계적 확인:**

```
$ grep -rn "ChatClient" src/main/java/com/skala/day1/web/
(일치하는 import 없음 — 컨트롤러가 ChatClient를 모른다)
```

---

## 3. 실행 결과 캡처 (Swagger UI)

실제 `OPENAI_API_KEY`로 서버를 기동해 Swagger UI(`/swagger-ui/index.html`)에서 Try it out으로 6가지 시나리오를 직접 호출하고 응답을 캡처했다.

### 3-1. 정상 응답 — 완료 기준 1번

`GET /lab1/orders/12345/summary?userId=user1` — 본인 소유 주문을 실제 키로 요약 요청.

![정상 응답](images/01_정상응답_200.png)

**결과**: 200 OK, `summary = "주문번호 12345의 무선 이어폰은 현재 배송 중이며, 도착 예정일은 2026년 8월 20일입니다."` — temperature 0으로 고정된 실제 모델 응답.

### 3-2. 권한 격리 — 완료 기준 2번

`GET /lab1/orders/99999/summary?userId=user1` — 99999는 user2 소유 주문.

![권한 격리](images/02_권한격리_404.png)

**결과**: 404 Not Found, `message = "주문을 찾을 수 없습니다."` — 남의 주문에 접근했을 때 모델을 부르지 않고 즉시 거부됨.

### 3-3. 존재하지 않는 주문 — 완료 기준 2번 보강

`GET /lab1/orders/00000/summary?userId=user1` — 아예 존재하지 않는 주문번호.

![존재하지 않는 주문](images/03_존재하지않는주문_404.png)

**결과**: 남의 주문(3-2)과 동일하게 404 — 존재 여부를 구분해서 알려주지 않는다(정보 노출 방지).

### 3-4. 폴백 동작 — 완료 기준 8번 (오늘의 핵심 학습 지점)

서버를 `OPENAI_API_KEY=dummy`로 재기동한 뒤 `GET /lab1/orders/12345/summary?userId=user1` 재호출.

![폴백 동작](images/04_폴백동작_200.png)

**결과**: 모델 호출이 실패해도 503이 아니라 200 OK, `summary = "무선 이어폰 · 배송 중"`(서비스 계층 폴백 문구) — AI 실패가 API 전체 실패로 번지지 않음을 실측 확인.

### 3-5. 재현성 — 완료 기준 5번

`GET /lab1/orders/12345/summary?userId=user1`을 실제 키로 19초 간격 3회 연속 호출.

![재현성 1회차](images/06_재현성_1회차.png)
![재현성 2회차](images/06_재현성_2회차.png)
![재현성 3회차](images/06_재현성_3회차.png)

**결과**: 07:27:26 / 07:27:45 / 07:27:55, 세 번 모두 `summary = "주문번호 12345의 무선 이어폰은 현재 배송 중이며, 도착 예정일은 2026년 8월 20일입니다."`로 완전히 동일 — `temperature=0.0` 고정이 재현성으로 이어짐을 실측 확인.

### 3-6. 503 + traceId — 완료 기준 7번

`OrderSummaryService.callModel()`은 평소 모델 호출 실패를 내부에서 잡아 폴백(200)으로 바꾸기 때문에, 정상 상태에서는 `Lab1ExceptionHandler`의 503 분기에 도달할 방법이 없다(이것이 8번 폴백이 의도한 동작이다). 503 응답 자체를 직접 확인하기 위해 `callModel()`의 `try/catch`를 **일시적으로 제거**해 예외가 컨트롤러까지 올라가도록 만든 뒤, `OPENAI_API_KEY=dummy`로 호출해 캡처하고 즉시 원상복구했다.

![503 + traceId](images/05_503_traceId.png)

**결과**: 503 Service Unavailable, `message = "요약을 만들지 못했습니다. 잠시 후 다시 시도해 주세요."`, `traceId = "faeafa11"` — 스택트레이스 없이 안전한 문구와 추적 ID만 노출됨을 확인. (⚠️ 이 캡처만 예외 처리 코드를 잠시 비활성화한 상태에서 얻었고, 캡처 직후 원래 코드로 복구했다. 저장소·제출 코드에는 반영되지 않았다.)

---

## 4. 완료 기준 체크리스트

| 번호 | 기준                                    | 결과                                    | 근거                     |
| ---- | --------------------------------------- | --------------------------------------- | ------------------------ |
| 1    | 엔드포인트 동작 (12345/user1 → 200)     | ✅ 확인                                 | 3-1 캡처                 |
| 2    | 권한 격리 (99999 → 404)                 | ✅ 확인                                 | 3-2 캡처                 |
| 3    | 계층 분리 (컨트롤러에 ChatClient 없음)  | ✅ 확인                                 | grep 결과 (2절)          |
| 4    | 빈 구성 (ChatClient 빈이 config에 하나) | ✅ 확인                                 | `Lab1AiConfig.java`      |
| 5    | 옵션 고정 (같은 입력 → 거의 같은 답)    | ✅ 확인                                 | 3-5 캡처 (3회 완전 동일) |
| 6    | 문서화 (Swagger 설명·예시·404)          | ✅ 확인                                 | 3절 캡처 전체            |
| 7    | 실패 처리 (모델 오류 시 503 + traceId)  | ✅ 확인 (예외처리 임시 비활성화로 재현) | 3-6 캡처                 |
| 8    | 폴백 (AI 실패해도 주문 정보는 나감)     | ✅ 확인                                 | 3-4 캡처                 |

8개 전부 화면으로 실측 확인했다. 단 7번은 정상 운영 코드에서는 도달하지 않는 경로라서, 확인을 위해 예외 처리를 일시적으로 비활성화하는 결함 주입(fault injection) 방식을 썼다는 점을 명시한다.

---

## 5. 협업 방식

계층 경계(Controller/Repository ↔ Service/Config)를 기준으로 역할을 나눴다. 갈라지기 전에 도메인 모델(`Order`, `OrderStatus`)과 저장소 시그니처, 응답 DTO를 먼저 계약으로 고정한 뒤 각자 맡은 파일을 구현하고 합쳤다.

## 6. 산출물 위치

GitHub: [https://github.com/wodnjs2020136144/day1-order-summary](https://github.com/wodnjs2020136144/day1-order-summary)
(`SpringAI_실습/07_주문요약_메인실습` 폴더를 별도 리포지토리로 분리)
