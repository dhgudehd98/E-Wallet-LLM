package com.sh.ewalletllm.llmclient.dto.gpt.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GptChoice {

    private String finish_reason; // 응답이 어떤 이유로 끝났는지 응답해주는 부분
    private GptResponseMessageDto message;
    /**
     * Gpt에서 Stream을 사용하는 경우에는 delta에 담아서 전달해줌.
     */
    private GptResponseMessageDto delta;
}