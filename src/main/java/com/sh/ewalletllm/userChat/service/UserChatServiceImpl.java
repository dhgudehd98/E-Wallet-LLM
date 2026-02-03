package com.sh.ewalletllm.userChat.service;

import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import com.sh.ewalletllm.llmclient.dto.LlmChatResponseDto;
import com.sh.ewalletllm.llmclient.service.LlmWebClientService;
import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import com.sh.ewalletllm.userChat.dto.UserChatResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserChatServiceImpl implements UserChatService{
    /**
     * UserChatServiceImpl
     * 1. 사용자가 보낸 요청을 LLM 형식에 맞게 변환
     *  - UserChatRequestDto -> LlmRequestDto
     * 2. LLM 형식에 맞게 변환 후 , LLMModel에 맞게 Request 값 변경
     *  - GPT : LlmRequestDto -> GptChatReqeustDto
     *  - Gemini : LlmRequestDto -> GeminiChatReqeustDto
     * 3. Response도 요청 값과 동일하게 변경
     *
     */

    private final Map<LlmType, LlmWebClientService> llmWebClientServiceMap;

    @Override
    public Flux<UserChatResponseDto> getChatStream(UserChatRequestDto chatRequestDto) {
        LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(chatRequestDto, "요청에 적절히 응답");
        Flux<LlmChatResponseDto> chatCompletionStream = llmWebClientServiceMap.get(chatRequestDto.getLlmModel().getLlmType()).getChatCompletionStream(llmChatRequestDto);
        return chatCompletionStream
                .map(responseDto -> new UserChatResponseDto(responseDto));
    }
}