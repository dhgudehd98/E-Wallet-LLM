package com.sh.ewalletllm.llmclient.dto;

import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LlmChatRequestDto {

    private String userRequest;
    private String systemPrompt;
    private boolean useJson;
    private LlmModel llmModel;

    /**
     * 사용자가 요청한 값을 LlmChatReqeustDto에 맞게 변환
     */
    public LlmChatRequestDto(UserChatRequestDto userChatRequestDto, String systemPrompt) {
        this.userRequest = userChatRequestDto.getRequest();
        this.llmModel = userChatRequestDto.getLlmModel();
        this.systemPrompt = systemPrompt;


    }
}