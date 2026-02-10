package com.sh.ewalletllm.userChat.service;

import com.sh.ewalletllm.api.service.AppClientService;
import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import com.sh.ewalletllm.llmclient.dto.LlmChatResponseDto;
import com.sh.ewalletllm.llmclient.service.LlmApplyService;
import com.sh.ewalletllm.llmclient.service.LlmReservationService;
import com.sh.ewalletllm.llmclient.service.LlmRetrieveService;
import com.sh.ewalletllm.llmclient.service.LlmWebClientService;
import com.sh.ewalletllm.userChat.dto.IntentDto;
import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import com.sh.ewalletllm.userChat.dto.UserChatResponseDto;
import com.sh.ewalletllm.userChat.utils.ChatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserChatServiceImpl implements UserChatService{
    /**
     * UserChatServiceImpl
     * 1. 사용자가 보낸 요청을 LLM 형식에 맞게 변환
     *  - UserChatRequestDto -> LlmRequestDto
     * 2. LLM 형식에 맞게 변환 후 , LLMModel에 맞게 Request 값 변경
     *  - GPT : LlmRequestDto -> GptChatReqeustDto
     *  - Gemini : LlmRequestDto -> GeminiChatReqeustDto
     * 3. Response도 요청 값과 동일하게 변경
     *
     */

    private final Map<LlmType, LlmWebClientService> llmWebClientServiceMap;
    private final LlmReservationService llmReservationService;
    private final LlmRetrieveService llmRetrieveService;
    private final LlmApplyService llmApplyService;
    private final ChatUtil chatUtil;

    @Override
    public Flux<UserChatResponseDto> getChatStream(UserChatRequestDto chatRequestDto) {
        LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(chatRequestDto, "요청에 적절히 응답");
        Flux<LlmChatResponseDto> chatCompletionStream = llmWebClientServiceMap.get(chatRequestDto.getLlmModel().getLlmType()).getChatCompletionStream(llmChatRequestDto);
        return chatCompletionStream
                .map(responseDto -> new UserChatResponseDto(responseDto));
    }

    /**
     * 1. 사용자의 요청은 환전신청 , 환전예약 , 환율 조회 관련된 기능만 검색할 수 있도록 설정
     * 2. OPEN AI한테 사용자가 요청한 값을 바탕으로 intent 값 입력(JSON 형식으로)
     * - intent : reservation (환전 예약) -> 요청한 값을 바탕으로 실제로 환전예약
     * - intent : RETRIEVE (환율 조회) -> 요청한 값을 바탕으로 환율 조회
     * - intent : APPLY (환전 신청) -> 요청한 값을 바탕으로 환전 신청
     */
    @Override
    public Flux<UserChatResponseDto> getChatIntent(UserChatRequestDto userChatRequestDto, String authHeader) {
        return getIntentJson(userChatRequestDto)
                .flatMapMany(intentDto->{
                    String intent = intentDto.getIntent();
                    return switch (intent) {
                        case "RESERVATION" -> llmReservationService.getReservationCommand(userChatRequestDto, authHeader);
                        case "RETRIEVE" -> llmRetrieveService.getRetrieveCommand(userChatRequestDto, authHeader);
                        case "APPLY" -> llmApplyService.getApplyCommand(userChatRequestDto, authHeader);
                        default-> Flux.just(
                                new UserChatResponseDto("요청을 이해하지 못했습니다. 요청은 환전신청 , 환전 예약, 환율 조회에 대한 기능만 조회 가능합니다.")
                        );

                    };
                });
    }

    private Mono<IntentDto> getIntentJson(UserChatRequestDto userChatRequestDto) {
        String systemPrompt = getIntentSystemPrompt(userChatRequestDto.getRequest());
        LlmModel llmModel = userChatRequestDto.getLlmModel();

        LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(userChatRequestDto, systemPrompt);
        LlmWebClientService llmWebClientService = llmWebClientServiceMap.get(llmModel.getLlmType());
        return llmWebClientService.getIntentDto(llmChatRequestDto)
                .map(responseDto -> chatUtil.parseJsonIntentDto(responseDto.getLlmResponse()));
    }


    // 시스템 프롬포트 작성
    private String getIntentSystemPrompt(String userRequest) {
        return String.format("""
            너는 사용자의 요청을 분석해서 "의도(intent)"만 판단하는 AI야.

            [사용자 요청]
            "%s"

            [Intent 판단 규칙]
            - APPLY: 지금 당장 환전을 실행해달라는 요청
              (예: "지금 USD 환전해줘", "환전 신청할게")
            
            - RESERVATION: 환전을 예약하라고 명확하게 지시한 경우
              (예: "USD 환전 예약해줘", "다음 달에 환전 예약 걸어줘")
            
            - RETRIEVE: 
              - 환율 조회
              - 환율 예측
              - 환율 전망
              - 환전 전에 정보나 의견을 묻는 경우
              ❗ "예약을 하려고 하는데", "환전할까 고민중인데" 처럼
                 실행 의도가 명확하지 않으면 반드시 RETRIEVE로 판단해.

            [중요 규칙]
            - 실제 환전 또는 예약을 **명확하게 실행하라는 표현이 없으면**
              절대 APPLY나 RESERVATION으로 판단하지 마.
            - 정보 조회, 예측, 의견 요청은 모두 RETRIEVE다.

            [출력 형식]
            반드시 아래 JSON 형식으로만 응답해.
            {
              "intent": "APPLY" | "RETRIEVE" | "RESERVATION"
            }
            """, userRequest);
    }

}