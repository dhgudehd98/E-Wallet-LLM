package com.sh.ewalletllm.api.service;


import com.sh.ewalletllm.api.dto.AppReservationResultDto;

import com.sh.ewalletllm.llmclient.dto.reservation.ResRequestDto;
import com.sh.ewalletllm.llmclient.dto.retrieve.RealTimeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppClientService {

    private final WebClient webClient;

    public Mono<AppReservationResultDto> appClientReservation(
            ResRequestDto resRequestDto,
            String authHeader
    ) {
        return webClient.post()
                .uri("http://localhost:8080/currency/reservation")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .bodyValue(resRequestDto)
                .retrieve()
                .bodyToMono(AppReservationResultDto.class);
    }

    public Flux<RealTimeDto> getCurrencyInfo() {
        return webClient.get()
                .uri("http://localhost:8080/llm/currencyInfo")
                .retrieve()
                .bodyToFlux(RealTimeDto.class);
    }
}