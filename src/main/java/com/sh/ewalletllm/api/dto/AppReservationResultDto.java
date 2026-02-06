package com.sh.ewalletllm.api.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AppReservationResultDto {
    private String result;
    private String msg;
    private String data;
}