# 07_주문요약_메인실습

**Day 1 메인 실습 · 주문 요약 API (p.94–101)**

강사 제공 샘플(01~06)과 달리 이 폴더는 대응하는 강사 샘플이 없다 — 미니 실습에서 익힌 조각
(3계층 왕복 = `01_간식추천_3계층`, 용도별 ChatClient 빈 = `04_말투바꾸기`)을 조합해 직접 구현한
결과물이다. 자세한 설계 배경은
`docs/SpringAI-이해-및-활용_Day1_2026-08/02_lab-guide.md`의 "메인 실습" 절 참조.

## 실행

```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun          # VS Code 는 F5
```

## 확인

```bash
curl 'localhost:8080/lab1/orders/12345/summary?userId=user1'   # 200
curl 'localhost:8080/lab1/orders/99999/summary?userId=user1'   # 404 — 99999는 user2 소유
```

Swagger UI — <http://localhost:8080/swagger-ui.html>

키 없이 도는 테스트만 돌리려면:

```bash
./gradlew test
```

## 이 폴더에 있는 것

- `domain/Order.java`, `domain/OrderStatus.java` — 안쪽 모델
- `config/Lab1AiConfig.java` — 요약 전용 `ChatClient` 빈 (온도 0 · maxTokens 120)
- `service/OrderRepository.java` — 인메모리 저장소 (고정 주문 4건)
- `service/OrderSummaryService.java` — 업무 흐름 + AI 호출 + 폴백
- `service/OrderNotFoundException.java`
- `web/OrderSummaryController.java` — `ChatClient`를 모른다
- `web/SummaryResponse.java`, `web/ErrorResponse.java`, `web/Lab1ExceptionHandler.java`

## 고정 데이터

| orderId | ownerId | item | status | eta |
|---|---|---|---|---|
| 12345 | user1 | 무선 이어폰 | 배송중 | 2026-08-20 |
| 12346 | user1 | 기계식 키보드 | 결제완료 | 2026-08-22 |
| 12347 | user1 | USB-C 케이블 | 배송완료 | 2026-08-15 |
| 99999 | user2 | 캠핑 의자 | 배송중 | 2026-08-21 |
