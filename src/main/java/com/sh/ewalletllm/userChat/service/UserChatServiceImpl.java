package com.sh.ewalletllm.userChat.service;

import com.sh.ewalletllm.api.service.AppClientService;
import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import com.sh.ewalletllm.llmclient.dto.LlmChatResponseDto;
import com.sh.ewalletllm.llmclient.service.LlmWebClientService;
import com.sh.ewalletllm.reservation.dto.ResRequestDto;
import com.sh.ewalletllm.reservation.dto.ValidationResult;
import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import com.sh.ewalletllm.userChat.dto.UserChatResponseDto;
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
    private final ObjectMapper objectMapper;
    private final AppClientService appClientService;

    @Override
    public Flux<UserChatResponseDto> getChatStream(UserChatRequestDto chatRequestDto) {
        LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(chatRequestDto, "요청에 적절히 응답");
        Flux<LlmChatResponseDto> chatCompletionStream = llmWebClientServiceMap.get(chatRequestDto.getLlmModel().getLlmType()).getChatCompletionStream(llmChatRequestDto);
        return chatCompletionStream
                .map(responseDto -> new UserChatResponseDto(responseDto));
    }

    /**
     * OPEN AI 통신 구조
     * 1. 사용자 요청에 맞게 AI 요청 -> AI 응답은 -> JSON 형태로 변환
     * 2. 요청 받은 데이터 유효성 검사 확인(필수값이 정상적으로 들어가있는지 확인하기)
     *   - 성공 시 , 3번으로
     *   - 실패 시 , 필요 값 응답 메세지로 전송
     * 3. 유효성 검사 통과 했으면 실제로 APP에서 환전 예약 진행
     *  - 환전 예약 성공 / 실패 여부에 따라서 메세지 전송 = 실제로 GPT 응답
     */
    @Override
    public Flux<UserChatResponseDto> getChatCommand(UserChatRequestDto chatRequestDto, String authHeader) {
        return requestCommandJson(chatRequestDto) //Mono<ResRequestDto>
                .map(response -> validateRequestDto(response)) //Mono<ValidationResult>
                .flatMapMany(result -> { //Flux<ValidationResult>

                    // 검증 실패 DTO에 없는 값 존재
                    if (!result.isResult()) {
                        return Flux.fromIterable(result.getErrors())
                                .map(errors -> new UserChatResponseDto(errors));
                    }

                    return appClientService.appClientReservation(result.getDto(), authHeader) // Mono<AppReservationResultDto>
                            .flatMapMany(reservationResult ->
                                    Flux.just(new UserChatResponseDto(reservationResult.getMsg()))
                            );

                });

    }

    /**
     * OPEN AI에서 만든 데이터 값 유효성 검사
     * 필수로 있어야되는 데이터 :
     * - currencyKind : 지불 통화 ,
     * - exchangeKind : 예약할 통화,
     * - inputExchangeMoney : 예약하고 싶은 환율의 필요 금액
     */
    private ValidationResult validateRequestDto(ResRequestDto resRequestDto) {
        log.info("[Validation ResRequestDto] RequestDto : " + resRequestDto);

        List<String> errors = new ArrayList<>();

        if (resRequestDto.getCurrencyKind() == null)
            errors.add("기준 통화가 없습니다.");

        if (resRequestDto.getExchangeKind() == null)
            errors.add("교환할 통화값이 존재하지 않습니다.");

        if (resRequestDto.getInputExchangeMoney() == null)
            errors.add("예약하고 싶은 통화의 교환 금액을 입력해주세요.");

        if (errors.isEmpty()) {
            return ValidationResult.success(resRequestDto);
        }

        return ValidationResult.fail(errors);

    }

    // OPEN AI -> 사용자가 입력한 요청 값 JSON으로 데이터 전달 요청
    private Mono<ResRequestDto> requestCommandJson(UserChatRequestDto userChatRequestDto) {
        String systemPrompt = buildSystemPrompt(userChatRequestDto.getRequest());
        LlmModel llmModel = userChatRequestDto.getLlmModel();

        LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(userChatRequestDto, systemPrompt);
        LlmWebClientService llmWebClientService = llmWebClientServiceMap.get(llmModel.getLlmType());
        return llmWebClientService.getCommandResRequestDto(llmChatRequestDto)
                .map(response -> parseJson(response.getLlmResponse()));
    }

    // 요청한 값 String -> JSON으로 변환
    private ResRequestDto parseJson(String json) {
        String jsonString = extractJsonString(json);
        log.info("[GPT RESPONSE TO JSON] : " + jsonString);
        try {
            return  objectMapper.readValue(jsonString, ResRequestDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("[parssing error] : String -> ResRequestDto Parsing Failed");
        }
    }

    private String extractJsonString(String content) {
        int firstIndex = content.indexOf('{');
        int lastIndex = content.lastIndexOf('}');

        log.info("[FirstIndex] : " + firstIndex);
        log.info("[lastIndex] : " + lastIndex);

        if (firstIndex != -1 && lastIndex != -1 && firstIndex < lastIndex) {
            log.info("[Extract Json String] : " + content.substring(firstIndex, lastIndex + 1));
            return content.substring(firstIndex, lastIndex + 1);
        }

        return "";
    }

    // 시스템 프롬포트 작성
    private String buildSystemPrompt(String userRequest) {
        String commandSystemPrompt = String.format("""
                       너는 환전과 관련된 작업을 실행해주는 AI야.
                       사용자의 요청은 "%s" 이고, 주로 환전 예약을 진행할 때는 USD , JPY , EUR에 대해서만 환전 예약을 할 수 있어.
                       그리고 실제로 데이터를 반환할 때는 JSON 형식으로 반환해줘 
                       환전 예약을 요청 할 때 아래 JSON FORMAT 형태로 응답해줘.
                       변수 설명 해줄게
                       currencyKind : 사용자가 지불 통화 , 없으면 KRW로 고정 
                       exchangeKind : 사용자가 예약하고자하는 환율의 통화 (USD , JPY , EUR 만 가능)
                       reservationRate : 사용자가 예약하고 싶은 환율 값 
                       inputExchangeMoney : 사용자가 예약하고자 하는 환율의 값 
                       예를 들어서 , 나 10USD 1400원으로 예약 해줘 하면 currencyKind에 대한 값은 없으니 KRW , inputExchangeMoney : 10 , exchangeKind : USD 
                       {
                           intent : RESERVATION
                           currencyKind : "", 
                           exchangeKind : "",
                           inputExchangeMoney : ,
                           reservationRate : ,
                           
                       }         
                       """, userRequest);

        return commandSystemPrompt;
    }


}