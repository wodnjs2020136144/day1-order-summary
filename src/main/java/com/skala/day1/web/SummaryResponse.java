package com.skala.day1.web;

import io.swagger.v3.oas.annotations.media.Schema;

/** 밖으로 나가는 모양. 안쪽 모델(Order)을 그대로 내보내지 않는다. */
public record SummaryResponse(
        @Schema(example = "12345") String orderId,
        @Schema(example = "무선 이어폰이 배송 중이며 2026-08-20 도착 예정입니다.") String summary) {}
