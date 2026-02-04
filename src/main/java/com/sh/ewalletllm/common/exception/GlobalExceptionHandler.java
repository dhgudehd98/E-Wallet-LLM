package com.sh.ewalletllm.common.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(GptErrorException.class)
    public Mono<ResponseError> handlerGptException(Exception ex, ServerWebExchange webExchange) {
        ServerHttpRequest request = webExchange.getRequest();

        log.error("[Open AI Exception] Request URI : {} , Method : {}, Error : {}", request.getURI(), request.getMethod(), ex.getMessage(), ex);
        return Mono.just(new ResponseError("500", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ResponseError> handlerGeneralException(Exception ex, ServerWebExchange webExchange) {
        ServerHttpRequest request = webExchange.getRequest();

        log.error("[General Exception] Request URI : {} , Method : {}, Error : {}", request.getURI(), request.getMethod(), ex.getMessage(), ex);
        return Mono.just(new ResponseError("500", ex.getMessage()));
    }
}