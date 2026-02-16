package com.sh.ewalletllm.llmclient.dto;

import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.redis.ChatMessageDto;
import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LlmChatRequestDto {

    private String userRequest;
    private String systemPrompt;
    private boolean useJson;
    private LlmModel llmModel;
    private List<ChatMessageDto> messageList;

    /**
     * 사용자가 요청한 값을 LlmChatReqeustDto에 맞게 변환
     */
    public LlmChatRequestDto(UserChatRequestDto userChatRequestDto, String systemPrompt) {
        this.userRequest = userChatRequestDto.getRequest();
        this.llmModel = userChatRequestDto.getLlmModel();
        this.systemPrompt = systemPrompt;

    }

    public LlmChatRequestDto(String systemPrompt, boolean useJson, LlmModel llmModel) {
        this.systemPrompt = systemPrompt;
        this.useJson = useJson;
        this.llmModel = llmModel;
    }

    public LlmChatRequestDto(UserChatRequestDto userChatRequestDto, String systemPrompt, List<ChatMessageDto> messageList) {
        this.userRequest = userChatRequestDto.getRequest();
        this.systemPrompt = systemPrompt;
        this.messageList = messageList;
        this.llmModel = userChatRequestDto.getLlmModel();
    }
}