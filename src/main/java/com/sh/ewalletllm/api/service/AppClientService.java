package com.sh.ewalletllm.api.service;


import com.sh.ewalletllm.api.dto.AppReservationResultDto;
import com.sh.ewalletllm.reservation.dto.ResRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppClientService {

    private final WebClient webClient;

    public Mono<AppReservationResultDto> appClientReservation(
            ResRequestDto resRequestDto
//            Authentication authentication
    ) {
        AppReservationResultDto reservationResultDto = new AppReservationResultDto();
        reservationResultDto.setMsg("환전 예약 설정이 완료되었습니다.");
        reservationResultDto.setResult("Y");

        return Mono.just(reservationResultDto);
//        return webClient.post()
//                .uri("http://localhost:8080/currency/reservation")
//                .bodyValue(resRequestDto)
//                .retrieve()
//                .bodyToMono(AppReservationResultDto.class);
    }
}