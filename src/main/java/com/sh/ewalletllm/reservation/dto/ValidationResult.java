package com.sh.ewalletllm.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class ValidationResult {

    private boolean result;
    private ResRequestDto dto;
    private List<String> errors;

    public static ValidationResult success(ResRequestDto dto) {
        return new ValidationResult(true, dto, List.of());
    }

    public static ValidationResult fail(List<String> errors) {
        return new ValidationResult(false, null, errors);
    }
}