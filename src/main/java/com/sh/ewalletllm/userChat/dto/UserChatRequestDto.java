package com.sh.ewalletllm.userChat.dto;

import com.sh.ewalletllm.llmclient.LlmModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserChatRequestDto {

    private String request;
    private LlmModel llmModel;

    @Override
    public String toString() {
        return "UserChatRequestDto{" +
                "request='" + request + '\'' +
                ", llmModel=" + llmModel +
                '}';
    }
}