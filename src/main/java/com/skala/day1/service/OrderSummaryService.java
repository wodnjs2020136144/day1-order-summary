package com.skala.day1.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.skala.day1.domain.Order;
import com.skala.day1.web.SummaryResponse;

/**
 * Day 1 메인 실습 — 업무 흐름은 여기에만 적는다.
 *
 * <p>주문을 못 찾으면 모델을 부르지 않는다. 모델 호출이 실패하거나 응답이 잘리면(finishReason=length),
 * 요약 없이 원본 주문 정보만이라도 돌려준다 — AI 실패가 전체 API 실패로 번지지 않게 한다.
 */
@Service
public class OrderSummaryService {

    private static final Logger log = LoggerFactory.getLogger(OrderSummaryService.class);

    private final OrderRepository orders;
    private final ChatClient summaryChat;

    public OrderSummaryService(OrderRepository orders,
                                @Qualifier("summaryChatClient") ChatClient summaryChatClient) {
        this.orders = orders;
        this.summaryChat = summaryChatClient;
    }

    public SummaryResponse summarize(String orderId, String userId) {
        Order order = orders.findByIdAndOwnerId(orderId, userId)   // 권한은 쿼리 안에
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return new SummaryResponse(order.id(), callModel(order));
    }

    /** AI가 죽어도 주문 정보는 보여 준다 — 요약은 부가 정보다. */
    private String callModel(Order order) {
        try {
            ChatResponse response = summaryChat.prompt()
                    .user(u -> u.text("주문번호 {id} · 상품 {item} · 상태 {status} · 도착예정 {eta}"
                                     + "\n위 정보를 한 문장으로 요약해 줘.")
                            .param("id", order.id())
                            .param("item", order.item())
                            .param("status", order.status().label())
                            .param("eta", order.eta()))
                    .call()
                    .chatResponse();

            String finishReason = response.getResult().getMetadata().getFinishReason();
            if ("LENGTH".equalsIgnoreCase(finishReason)) {
                log.warn("[{}] 요약 응답이 maxTokens 상한에서 잘림 — 폴백으로 대체", order.id());
                return fallback(order);
            }
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.warn("[{}] 요약 모델 호출 실패 — 폴백으로 대체: {}", order.id(), e.getMessage());
            return fallback(order);
        }
    }

    private String fallback(Order order) {
        return order.item() + " · " + order.status().label();
    }
}
