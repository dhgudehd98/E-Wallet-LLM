package com.sh.ewalletllm.redis;

import com.sh.ewalletllm.llmclient.dto.gpt.request.GptMessageRole;
import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private GptMessageRole role;
    private String content;
    private Long timeStamp;

    public ChatMessageDto(UserChatRequestDto chatRequestDto) {
        this.role = GptMessageRole.USER;
        this.content = chatRequestDto.getRequest();
        this.timeStamp = System.currentTimeMillis();
    }
}