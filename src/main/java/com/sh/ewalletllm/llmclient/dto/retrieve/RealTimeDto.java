package com.sh.ewalletllm.llmclient.dto.retrieve;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RealTimeDto {
    private String code; // 통화 코드
    private double price; // 실시간 환율 (1000원 기준)
    private Double prevClose; // 전일 종가
    private Double diff; // 변화량
    private Double diffPct; // 증감률
}