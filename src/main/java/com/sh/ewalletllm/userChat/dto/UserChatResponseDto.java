package com.sh.ewalletllm.userChat.dto;

import com.sh.ewalletllm.common.exception.ResponseError;
import com.sh.ewalletllm.llmclient.dto.LlmChatResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserChatResponseDto {

    private String response;
    private ResponseError responseError;

    public UserChatResponseDto(LlmChatResponseDto llmChatResponseDto) {
        this.response = llmChatResponseDto.getLlmResponse();
        this.responseError = llmChatResponseDto.getResponseError();
    }

    public UserChatResponseDto(String errors) {
        this.response = errors
        ;
    }

    public static UserChatResponseDto getMessage(String message) {
        return new UserChatResponseDto(message);
    }


}