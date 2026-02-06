package com.sh.ewalletllm.llmclient.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class ReservationValidationResult {

    private boolean result;
    private ResRequestDto dto;
    private List<String> errors;

    public static ReservationValidationResult success(ResRequestDto dto) {
        return new ReservationValidationResult(true, dto, List.of());
    }

    public static ReservationValidationResult fail(List<String> errors) {
        return new ReservationValidationResult(false, null, errors);
    }
}