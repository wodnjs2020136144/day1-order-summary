package com.skala.day1.domain;

/** 주문 상태 — 프롬프트에 넣을 한국어 표기를 함께 들고 다닌다. */
public enum OrderStatus {

    결제완료("결제 완료"),
    배송중("배송 중"),
    배송완료("배송 완료"),
    취소("취소됨");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
