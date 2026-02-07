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
    private String currency;
    private Long amount;
}