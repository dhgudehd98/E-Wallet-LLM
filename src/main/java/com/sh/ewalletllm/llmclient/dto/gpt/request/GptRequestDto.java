package com.sh.ewalletllm.llmclient.dto.gpt.request;

import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class GptRequestDto {

    private List<GptRequestCompletionDto> messages;
    private LlmModel model;
    private Boolean stream;
    private GptResponseFormat response_format;

    public GptRequestDto(LlmChatRequestDto llmChatRequestDto) {
        if(llmChatRequestDto.isUseJson()) {
            this.response_format = new GptResponseFormat("json_object");
        }
        this.messages = List.of(
                new GptRequestCompletionDto(GptMessageRole.SYSTEM, llmChatRequestDto.getSystemPrompt()),
                new GptRequestCompletionDto(GptMessageRole.USER, llmChatRequestDto.getUserRequest())
        );

        this.model = llmChatRequestDto.getLlmModel();

    }
}