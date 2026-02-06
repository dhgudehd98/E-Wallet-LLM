package com.sh.ewalletllm.llmclient.service;


import com.sh.ewalletllm.api.service.AppClientService;
import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import com.sh.ewalletllm.llmclient.dto.LlmChatResponseDto;
import com.sh.ewalletllm.llmclient.dto.retrieve.RealTimeDto;
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

    /**
     * 환율 조회에 대한 과정
     * 1. 실제 환율을 서버에서 받아온다. -> APPCLIENTSERVICE 쪽에서
     * 2. 받아온 환율 데이터를 통해서 AI한테 전달하고, GPT의 응답에 가져오기
     * @param userChatRequestDto
     * @return
     */

    public Flux<UserChatResponseDto> getRetrieveCommand(UserChatRequestDto userChatRequestDto) {
        return getCurrencyInfo()
                .collectList()
                .flatMapMany(
                        realTimeDtoList -> sendCurrencyInfoToAi(userChatRequestDto, realTimeDtoList)
                ).map(
                        llmResponse -> new UserChatResponseDto(llmResponse.getLlmResponse())
                );

    }

    private Flux<LlmChatResponseDto> sendCurrencyInfoToAi(UserChatRequestDto userChatRequestDto, List<RealTimeDto> realTimeDtoList) {
        String userRequest = userChatRequestDto.getRequest();
        String systemPrompt = retrieveSystemPrompt(userRequest, realTimeDtoList);
        LlmModel llmModel = userChatRequestDto.getLlmModel();

        LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(userChatRequestDto, systemPrompt);
        LlmWebClientService llmWebClientService = llmWebClientServiceMap.get(llmModel.getLlmType());

        return llmWebClientService.getRetrieveCommand(llmChatRequestDto);

    }

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

            [실시간 환율 데이터]
            %s
            """,
                userRequest,
                currencyInfo.toString()
        );
    }

    // APP으로 실시간 환율 요청
    private Flux<RealTimeDto> getCurrencyInfo() {
        return appClientService.getCurrencyInfo();
    }
}