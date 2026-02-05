package com.sh.ewalletllm.userChat.service;

import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import com.sh.ewalletllm.userChat.dto.UserChatResponseDto;
import reactor.core.publisher.Flux;

public interface UserChatService {
    Flux<UserChatResponseDto> getChatStream(UserChatRequestDto chatRequestDto);

    Flux<UserChatResponseDto> getChatCommand(UserChatRequestDto chatRequestDto);
}