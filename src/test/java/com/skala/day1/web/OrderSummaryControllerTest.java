package com.skala.day1.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.skala.day1.service.OrderNotFoundException;
import com.skala.day1.service.OrderSummaryService;

/**
 * 웹 계층만 확인 — 모델을 부르지 않으므로 키 없이 돈다.
 * {@code OrderSummaryService}를 Mockito로 대체해 컨트롤러의 상태코드 매핑만 검증한다.
 */
@WebMvcTest(OrderSummaryController.class)
class OrderSummaryControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderSummaryService service;

    @Test
    void 정상_주문은_200과_요약을_돌려준다() throws Exception {
        given(service.summarize("12345", "user1"))
                .willReturn(new SummaryResponse("12345", "무선 이어폰이 배송 중입니다."));

        mvc.perform(get("/lab1/orders/12345/summary").param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("12345"))
                .andExpect(jsonPath("$.summary").value("무선 이어폰이 배송 중입니다."));
    }

    @Test
    void 남의_주문이거나_없는_주문은_404다() throws Exception {
        given(service.summarize("99999", "user1")).willThrow(new OrderNotFoundException("99999"));

        mvc.perform(get("/lab1/orders/99999/summary").param("userId", "user1"))
                .andExpect(status().isNotFound());
    }
}
