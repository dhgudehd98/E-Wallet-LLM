package com.sh.ewalletllm.llmclient.dto.Apply;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserApplyInfoDto {
    String currencyKind; // 기준 환율 종류
    double currencyRate; // 기준 환율

    String exchangeKind; // 교환할 환율 종류
    double exchangeRate; // 기준 환율

    Long inputExchangeMoney; // 교환 환율 금액
    Long needCurrencyMoney; // 필요 금액
}