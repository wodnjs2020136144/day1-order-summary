package com.skala.day1.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.skala.day1.domain.Order;
import com.skala.day1.domain.OrderStatus;

/**
 * Day 1 메인 실습 — 데이터에 닿는 유일한 자리.
 *
 * <p>실제 DB는 쓰지 않는다(인메모리 Map). 오늘 학습 지점은 AI 계층이지 저장소가 아니다.
 * 나중에 JPA로 바꿔도 위 계층은 그대로다.
 */
@Repository
public class OrderRepository {

    private static final Map<String, Order> 주문 = Map.of(
            "12345", new Order("12345", "user1", "무선 이어폰", OrderStatus.배송중, "2026-08-20"),
            "12346", new Order("12346", "user1", "기계식 키보드", OrderStatus.결제완료, "2026-08-22"),
            "12347", new Order("12347", "user1", "USB-C 케이블", OrderStatus.배송완료, "2026-08-15"),
            "99999", new Order("99999", "user2", "캠핑 의자", OrderStatus.배송중, "2026-08-21"));

    /** 주문번호 + 소유자를 함께 조건으로 건다 — 권한은 쿼리 안에서 걸러진다. */
    public Optional<Order> findByIdAndOwnerId(String orderId, String ownerId) {
        return Optional.ofNullable(주문.get(orderId))
                .filter(o -> o.ownerId().equals(ownerId));
    }
}
