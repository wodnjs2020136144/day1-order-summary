package com.skala.day1.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Day 1 메인 실습 — 용도별로 나눈다: 요약 전용 클라이언트 하나.
 *
 * <p>온도와 토큰 상한을 여기서 못 박아 둔다. 호출부마다 정하게 두면 누군가는 기본값(0.7)으로
 * 부르고, 그날부터 요약이 매번 달라진다.
 */
@Configuration
class Lab1AiConfig {

    @Bean
    ChatClient summaryChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        너는 이커머스 주문 상담 도우미다.
                        주어진 주문 정보만 사용해 한국어 한 문장으로 요약한다.
                        추측하지 않는다. 정보가 부족하면 "정보가 부족합니다"라고 답한다.
                        """)
                // 이 프로젝트는 spring-ai-bom 2.0.0 을 쓴다 — defaultOptions는 ChatOptions.Builder를
                // 받는다(빌드 전 상태로 넘긴다). spring-ai 1.1.8 기반 미니 실습(04 등)의 .build() 호출과
                // 다르니 그대로 베끼지 않는다.
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.0)   // 요약은 매번 같아야 한다
                        .maxTokens(120))    // 비용 상한 — 길게 쓸 이유가 없다
                .build();
    }
}
