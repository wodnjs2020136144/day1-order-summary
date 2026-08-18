package com.skala.day1.service;

/** 주문이 없거나 조회 주체(userId)와 소유자가 다를 때 던진다 — 둘을 구분하지 않는다. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("주문을 찾을 수 없습니다: " + orderId);
    }
}
