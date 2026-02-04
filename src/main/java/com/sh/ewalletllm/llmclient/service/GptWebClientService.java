package com.sh.ewalletllm.llmclient.service;

import com.sh.ewalletllm.common.exception.GptErrorException;
import com.sh.ewalletllm.common.exception.ResponseError;
import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import com.sh.ewalletllm.llmclient.dto.LlmChatResponseDto;
import com.sh.ewalletllm.llmclient.dto.gpt.request.GptRequestDto;
import com.sh.ewalletllm.llmclient.dto.gpt.response.GptResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class GptWebClientService implements LlmWebClientService{

    private final WebClient webClient;

    @Value("${llm.gpt.key}")
    private String gptApiKey;
    @Override
    public Mono<LlmChatResponseDto> getChatCompletion(LlmChatRequestDto llmChatRequestDto) {
        return null;
    }

    @Override
    public LlmType getLlmType() {
        return LlmType.GPT;
    }

    @Override
    public Flux<LlmChatResponseDto> getChatCompletionStream(LlmChatRequestDto llmChatRequestDto) {
        GptRequestDto gptRequestDto = new GptRequestDto(llmChatRequestDto);
        AtomicInteger count = new AtomicInteger(0);
        gptRequestDto.setStream(true);
        return webClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + gptApiKey)
                .bodyValue(gptRequestDto)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (clientResponse -> {
                    return clientResponse.bodyToMono(String.class).flatMap(body -> {
                        return Mono.error(new GptErrorException("API 요청실패 : " + body));
                    });
                }))
                .bodyToFlux(GptResponseDto.class)
                .takeWhile(response -> Optional.ofNullable(response.getSingleChoice().getFinish_reason()).isEmpty())
                .map(gptResponseDto -> {
                    try {
                        return LlmChatResponseDto.getLlmChatResponseDtoFromStream(gptResponseDto);
                    } catch (Exception e) {
                        log.error("[GPT Response Error] : " + e.getMessage());
                        return new LlmChatResponseDto(new ResponseError("500" , e.getMessage()));
                    }
                });
//                .map(gptChatResponseDto -> LlmChatResponseDto.getLlmChatResponseDtoFromStream(gptChatResponseDto));

    }
}