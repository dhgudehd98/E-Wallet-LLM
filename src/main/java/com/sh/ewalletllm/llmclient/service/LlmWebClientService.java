package com.sh.ewalletllm.llmclient.service;

import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import com.sh.ewalletllm.llmclient.dto.LlmChatResponseDto;
import com.sh.ewalletllm.userChat.dto.UserChatResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LlmWebClientService {

    Mono<LlmChatResponseDto> getChatCompletion(LlmChatRequestDto llmChatRequestDto);

    LlmType getLlmType();

    Flux<LlmChatResponseDto> getChatCompletionStream(LlmChatRequestDto llmChatRequestDto);

    Mono<LlmChatResponseDto> getCommandResRequestDto(LlmChatRequestDto llmChatRequestDto);

    Mono<LlmChatResponseDto> getIntentDto(LlmChatRequestDto llmChatRequestDto);

    Flux<LlmChatResponseDto> getRetrieveCommand(LlmChatRequestDto llmChatRequestDto);

    Mono<LlmChatResponseDto> getUserRequestApplyInfo(LlmChatRequestDto llmChatRequestDto);
}
