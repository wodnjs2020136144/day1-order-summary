package com.skala.day1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import com.skala.day1.web.SummaryResponse;

/**
 * 완료 기준 8번(폴백) — AI가 실패해도 주문 정보는 나가야 한다는 것을 코드로 잡는다.
 * ChatClient의 유창한 API 체인을 직접 목킹하기보다 mock의 기본 동작(null 반환 → NPE)을
 * "모델 실패"로 취급해 폴백 경로를 검증한다.
 */
class OrderSummaryServiceTest {

    private OrderRepository orders;

    @BeforeEach
    void setUp() {
        orders = new OrderRepository();
    }

    @Test
    void 없는_주문이거나_남의_주문이면_모델을_부르지_않고_예외를_던진다() {
        ChatClient 절대_호출되면_안됨 = mock(ChatClient.class, invocation -> {
            throw new AssertionError("모델을 호출해서는 안 된다: " + invocation.getMethod());
        });
        OrderSummaryService service = new OrderSummaryService(orders, 절대_호출되면_안됨);

        assertThatThrownBy(() -> service.summarize("99999", "user1"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void 모델_호출이_실패해도_주문_정보는_200으로_나간다() {
        // 체이닝 도중 예외를 던지도록 만들어 "모델 호출 실패"를 재현한다.
        ChatClient 고장난_클라이언트 = mock(ChatClient.class);
        given(고장난_클라이언트.prompt()).willThrow(new RuntimeException("모델 호출 실패(재현용)"));

        OrderSummaryService service = new OrderSummaryService(orders, 고장난_클라이언트);

        SummaryResponse response = service.summarize("12345", "user1");

        assertThat(response.orderId()).isEqualTo("12345");
        assertThat(response.summary()).contains("무선 이어폰").contains("배송 중");   // 폴백 문구
    }
}
