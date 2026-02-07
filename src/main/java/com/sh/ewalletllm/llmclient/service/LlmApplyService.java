package com.sh.ewalletllm.llmclient.service;

import com.sh.ewalletllm.api.service.AppClientService;
import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.Apply.UserApplyInfoDto;
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

import java.util.Map;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class LlmApplyService {
    private final Map<LlmType, LlmWebClientService> llmWebClientServiceMap;
    private final ChatUtil chatUtil;
    private final AppClientService appClientService;
    private final LlmRetrieveService llmRetrieveService;

    /**
     * 환전 신청 플로우 구성
     * 1. 사용자 요청(intent) -> 환전신청에 대한 부분 확인
     * 2. 환전 신청 -> 사용자 요청을 분석해서 어떤 값을 가져올지 추출(AI한테 요청)
     * 3. 요청한 값 확인 -> 실시간 환율 값과 비교해서 실시간 환율과 다르면 환전 신청 X -> 메세지 출력
     * 4. 실시간 환율 값과 동일하면 환전신청

     */
    public Flux<UserChatResponseDto> getApplyCommand(UserChatRequestDto userChatRequestDto, String authHeader) {
        return getUserRequestApplyInfo(userChatRequestDto) // Mono<UserApplyInfoDto>
                .flatMap(userApplyInfoDto -> appClientService.appClientApply(userApplyInfoDto, authHeader)) // Mono<AppApplyResultDto>
                .flatMapMany(appApplyResultDto ->
                        Flux.just(new UserChatResponseDto(appApplyResultDto.getMsg())));

    }

    private Mono<UserApplyInfoDto> getUserRequestApplyInfo(UserChatRequestDto userChatRequestDto) {
        String userRequest = userChatRequestDto.getRequest();
        String systemPrompt = getUserRequestApplyPrompt(userRequest);
        LlmModel llmModel = userChatRequestDto.getLlmModel();

        LlmWebClientService llmWebClientService = llmWebClientServiceMap.get(llmModel.getLlmType());
        LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(userChatRequestDto, systemPrompt);

        return llmWebClientService.getUserRequestApplyInfo(llmChatRequestDto)
                .map(userApplyInfoDto -> chatUtil.parseUserApplyInfoDto(userApplyInfoDto.getLlmResponse()));
    }


    private String getUserRequestApplyPrompt(String userRequest) {
        return String.format("""
            너는 사용자의 환전 신청 요청에서
            "환전 실행에 필요한 정보만" 추출하는 AI야.

            [사용자 요청]
            "%s"

            [추출 규칙]
            - 통화 코드는 ISO-4217 형식 (USD, EUR, JPY)
            - 환전 금액은 숫자만 추출
            - 명확하지 않으면 null로 반환
            - 추측하지 마
            - 사용자가 환전신청 할때 목표금액 즉 , 1500원에 환율해줘 이런 값이 있으면 currencyRequest에 넣어줘, 만약에 없으면 null 값으로 넣어줘

            [출력 형식]
            반드시 아래 JSON 형식으로만 응답해.
            {
              "currency": "USD" | "EUR" | "JPY" | null,
              "amount": number | null
            }
            """, userRequest);
    }
}