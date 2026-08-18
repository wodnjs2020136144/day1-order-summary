package com.skala.day1.domain;

/** Day 1 메인 실습 — 밖으로 나가지 않는 안쪽 모델. */
public record Order(String id, String ownerId, String item, OrderStatus status, String eta) {}
