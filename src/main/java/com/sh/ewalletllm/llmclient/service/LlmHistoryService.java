package com.sh.ewalletllm.llmclient.service;

import com.sh.ewalletllm.chathistory.service.UserChatHistoryService;
import com.sh.ewalletllm.jwt.JwtUtil;
import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import com.sh.ewalletllm.llmclient.dto.retrieve.RealTimeDto;
import com.sh.ewalletllm.redis.ChatMessageDto;
import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import com.sh.ewalletllm.userChat.dto.UserChatResponseDto;
import com.sh.ewalletllm.userChat.utils.ChatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmHistoryService {
    private final Map<LlmType, LlmWebClientService> llmWebClientServiceMap;
    private final JwtUtil jwtUtil;
    private final UserChatHistoryService userChatHistoryService;
    public Flux<UserChatResponseDto> getHistoryCommand(UserChatRequestDto userChatRequestDto, String authHeader) {
        String systemPrompt = historySystemPrompt(userChatRequestDto.getRequest());
        LlmModel llmModel = userChatRequestDto.getLlmModel();

        Long memberId = jwtUtil.extractMemberId(authHeader);
        return userChatHistoryService.getRecentHistory(memberId)
                .collectList()
                .flatMapMany(historyList -> {
                    LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(userChatRequestDto, systemPrompt, historyList);
                    LlmWebClientService llmWebClientService = llmWebClientServiceMap.get(llmModel.getLlmType());

                    return llmWebClientService.getHisotryCommand(llmChatRequestDto)
                            .map(llmChatREsponseDto -> new UserChatResponseDto(llmChatREsponseDto.getLlmResponse()));
                });
    }


    private String historySystemPrompt(String userRequest) {
        return String.format("""
        너는 사용자와의 대화 내역을 기억하는 금융 AI 어시스턴트야.
        
        [사용자 질문]
        "%s"
        
        [응답 규칙]
        1. 이전 대화 내역(히스토리)을 바탕으로 사용자의 질문에 답해줘.
        2. 히스토리에 있는 내용만 답변하고 없는 내용은 절대 추측하지 마.
        3. 히스토리가 없거나 관련 내용이 없으면 솔직하게 없다고 말해줘.
        4. 답변은 이해하기 쉽게 자연어로 작성해.
        5. 말투는 친절하게 해줘.
        
        [주의]
        - 환율 데이터나 실시간 정보는 제공하지 마.
        - 오직 이전 대화 내역만을 기반으로 답변해.
        """,
                userRequest
        );
    }
}