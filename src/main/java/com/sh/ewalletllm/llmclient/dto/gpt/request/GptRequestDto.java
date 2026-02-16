package com.sh.ewalletllm.llmclient.dto.gpt.request;

import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import com.sh.ewalletllm.redis.ChatMessageDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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
        this.messages = new ArrayList<>();

        if(llmChatRequestDto.isUseJson()) {
            this.response_format = new GptResponseFormat("json_object");
        }

        this.messages.add(new GptRequestCompletionDto(GptMessageRole.SYSTEM, llmChatRequestDto.getSystemPrompt()));

        //Redis History 내역 가져오기
        List<ChatMessageDto> messageList = llmChatRequestDto.getMessageList();

        if (messageList != null && !messageList.isEmpty()) {
            for (ChatMessageDto messages : messageList) {
                this.messages.add(new GptRequestCompletionDto(messages.getRole(), messages.getContent()));
            }
        }

        // 마지막에 System 응답 요청 값
        this.messages.add(new GptRequestCompletionDto(GptMessageRole.USER, llmChatRequestDto.getUserRequest()));

        this.model = llmChatRequestDto.getLlmModel();

    }
}
