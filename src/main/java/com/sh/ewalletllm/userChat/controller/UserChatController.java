package com.sh.ewalletllm.userChat.controller;

import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import com.sh.ewalletllm.userChat.dto.UserChatResponseDto;
import com.sh.ewalletllm.userChat.service.UserChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class UserChatController {

    private final UserChatService userChatService;
    @PostMapping("/stream")
    public Flux<UserChatResponseDto> getChatStream(
            @RequestBody UserChatRequestDto chatRequestDto){
        log.info("Front End INit ");
        log.info("Data  :" + chatRequestDto.toString());
        return userChatService.getChatStream(chatRequestDto);
    }

    @PostMapping("/streamCommand")
    public Flux<UserChatResponseDto> getChatCommand(
            @RequestBody UserChatRequestDto chatRequestDto,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader){
        log.info("Front End INit ");
        log.info("Data  :" + chatRequestDto.toString());
        return userChatService.getChatCommand(chatRequestDto, authHeader);
    }
}