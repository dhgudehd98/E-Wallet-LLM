package com.sh.ewalletllm.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResRequestDto {

    private String intent;
    private String currencyKind; // 기준 환율 종류
    private String exchangeKind; // 교환할 환율 종류
    private Long inputExchangeMoney; // 교환 환율 금액
    private BigDecimal reservationRate; // 예약 환율
    private LocalDate startDate; // 예약 시작일
    private LocalDate endDate; // 예약 종료일


}