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
                        Flux.just(
                                new UserChatResponseDto(appApplyResultDto.getMsg() + "\n"),
                                new UserChatResponseDto(appApplyResultDto.getData())
                        ));

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
        **환전 실행에 필요한 값만 추출하는 AI**야.
        계산이나 판단은 하지 말고, 요청에 포함된 정보만 구조화해.

        [사용자 요청]
        "%s"

        [추출 규칙]
        - 통화 코드는 ISO-4217 형식 (USD, EUR, JPY)
        - 환전 금액은 숫자만 추출
        - 명확하지 않으면 통화에 대한 값은 null 값으로 반환하고, 환율에 대한 값은 0.0값으로 반환
        - 추측하지

        [필드 설명]
        - currencyKind: 기준 통화
          · 사용자가 기준 통화를 명시하지 않으면 "KRW"로 설정해.
        - currencyRate: 기준 통화의 환율 값
          · 계산하지 말고 항상 0.0으로 둬.
        - exchangeKind: 교환하려는 통화
        - exchangeRate: 교환 하려는 통화의 환율 값
            · 계산하지 말고 항상 0.0으로 둬.
        - inputExchangeMoney: 교환하려는 통화의 금액
        - needCurrencyMoney: 계산 값이므로 항상 0 값으로 설정

        [예시]
        1) 사용자 요청: "10 USD 환전 신청해줘"
           → currencyKind: "KRW"
             currencyRate: 0.0
             exchangeKind: "USD"
             exchangeRate: 0.0
             inputExchangeMoney: 10

        2) 사용자 요청: "10 USD를 EUR로 환전을 해줘"
           → currencyKind: "EUR"
             currencyRate: 0.0
             exchangeKind: "USD"
             exchangeRate: 0.0
             inputExchangeMoney: 10

        [출력 형식]
        반드시 아래 JSON 형식으로만 응답해.
        {
          "currencyKind": "KRW" | "USD" | "EUR" | "JPY",
          "currencyRate": 0.0,
          "exchangeKind": "USD" | "EUR" | "JPY" | null,
          "exchangeRate": 0.0,
          "inputExchangeMoney": number | null,
          "needCurrencyMoney": 0
        }
        """, userRequest);
    }
}