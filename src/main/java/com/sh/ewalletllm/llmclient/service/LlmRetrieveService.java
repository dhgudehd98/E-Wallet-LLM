package com.sh.ewalletllm.llmclient.service;


import com.sh.ewalletllm.api.service.AppClientService;
import com.sh.ewalletllm.chathistory.service.UserChatHistoryService;
import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import com.sh.ewalletllm.llmclient.dto.LlmChatResponseDto;
import com.sh.ewalletllm.llmclient.dto.retrieve.RealTimeDto;
import com.sh.ewalletllm.redis.ChatMessageDto;
import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import com.sh.ewalletllm.userChat.dto.UserChatResponseDto;
import com.sh.ewalletllm.userChat.utils.ChatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmRetrieveService {
    private final Map<LlmType, LlmWebClientService> llmWebClientServiceMap;
    private final ChatUtil chatUtil;
    private final AppClientService appClientService;
    private final UserChatHistoryService userChatHistoryService; // ✅ 추가

    public Flux<UserChatResponseDto> getRetrieveCommand(UserChatRequestDto userChatRequestDto, String authHeader) {
        Long memberId = 102L;

        return getCurrencyInfo(authHeader)
                .collectList()
                .zipWith(userChatHistoryService.getRecentHistory(memberId).collectList()) // ✅ 환율정보 + 히스토리 동시에 가져오기
                .flatMapMany(tuple -> {
                    List<RealTimeDto> realTimeDtoList = tuple.getT1();
                    List<ChatMessageDto> historyList = tuple.getT2();
                    return sendCurrencyInfoToAi(userChatRequestDto, realTimeDtoList, historyList); // ✅ 히스토리 전달
                })
                .map(llmResponse -> new UserChatResponseDto(llmResponse.getLlmResponse()));
    }

    private Flux<LlmChatResponseDto> sendCurrencyInfoToAi(UserChatRequestDto userChatRequestDto, List<RealTimeDto> realTimeDtoList, List<ChatMessageDto> historyList) {
        String userRequest = userChatRequestDto.getRequest();
        String systemPrompt = retrieveSystemPrompt(userRequest, realTimeDtoList);
        LlmModel llmModel = userChatRequestDto.getLlmModel();

        LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(userChatRequestDto, systemPrompt, historyList); // ✅ 히스토리 포함
        LlmWebClientService llmWebClientService = llmWebClientServiceMap.get(llmModel.getLlmType());

        return llmWebClientService.getRetrieveCommand(llmChatRequestDto);
    }

    // retrieveSystemPrompt, getCurrencyInfo 는 그대로 유지


    private String retrieveSystemPrompt(String userRequest, List<RealTimeDto> realTimeDtos) {

        StringBuilder currencyInfo = new StringBuilder();

        for (RealTimeDto dto : realTimeDtos) {
            currencyInfo.append(String.format("""
                - 통화: %s
                  · 실시간 환율: %.2f
                  · 전일 종가: %.2f
                  · 변화량: %.2f
                  · 증감률: %.2f%%
                """,
                    dto.getCode(),
                    dto.getPrice(),
                    dto.getPrevClose(),
                    dto.getDiff(),
                    dto.getDiffPct()
            ));
        }

        return String.format("""
            너는 실시간 환율 정보를 사용자에게 설명해주는 금융 AI 어시스턴트야.

            [사용자 질문]
            "%s"

            [응답 규칙]
            1. 사용자가 "환율 얼마야?", "JPY 환율 알려줘" 처럼 단순 조회를 요청하면
               → 숫자 위주로 간단하게 응답해.
            2. 사용자가 "지금 환전 어때?", "환율 전망 어때?" 처럼 의견을 요청하면
               → 아래 데이터를 기반으로 간단한 분석과 의견을 제공해.
            3. 데이터에 없는 내용은 절대 추측하지 마.
            4. 답변은 이해하기 쉽게 자연어로 작성해.
            5. 그리고 답변 할 때 말투는 친절하게 답변해줬으면 좋겠어 너무 딱딱하게 말고
            6. 이전에 요청한 값이 있으면 그거에 맞춰서 전달해줘

            [실시간 환율 데이터]
            %s
            """,
                userRequest,
                currencyInfo.toString()
        );
    }

    // APP으로 실시간 환율 요청
    public Flux<RealTimeDto> getCurrencyInfo(String authHeader) {
        return appClientService.getCurrencyInfo(authHeader);
    }
}