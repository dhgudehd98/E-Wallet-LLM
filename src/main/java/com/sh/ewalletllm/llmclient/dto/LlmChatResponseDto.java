package com.sh.ewalletllm.llmclient.dto;

import com.sh.ewalletllm.llmclient.dto.gpt.response.GptResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LlmChatResponseDto {

    private String llmResponse;

    public static LlmChatResponseDto getLlmChatResponseDtoFromStream(GptResponseDto gptChatResponseDto) {
        return new LlmChatResponseDto(gptChatResponseDto.getSingleChoice().getDelta().getContent());
    }
}