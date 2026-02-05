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
     * 1. 환전 예약 형식 전달
     * 2. 사용자 요청 값 확인하고, GPT에서 Response 값을 확인함.
     * 3. 확인한 다음 이상 없으면 -> 실제로 Spring Tomcat 서버에서 DB 저장
     * 4. Spring Tomcat에서 성공 / 실패 여부 메세지 전달 받음.
     *      - 환전 예약 성공 -> 챗봇 화면에 성공적으로 예약이 완료된거랑 , 요청한 환결정보 출력해주기
     *      - 환전 예약 실패 -> 챗봇 화면에 실패했다고, 알려주고 , 실패된 이유 출력
     *
     */
    @Override
    public Flux<UserChatResponseDto> getChatCommand(UserChatRequestDto chatRequestDto) {
        return requestCommandJson(chatRequestDto) //Mono<ResRequestDto>
                .map(response -> validateRequestDto(response)) //Mono<ValidationResult>
                .flatMapMany(result -> { //Flux<ValidationResult>

                    // 검증 실패 DTO에 없는 값 존재
                    if (!result.isResult()) {
                        return Flux.fromIterable(result.getErrors())
                                .map(errors -> new UserChatResponseDto(errors));
                    }

                    return appClientService.appClientReservation(result.getDto()) // Mono<AppReservationResultDto>
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

        if (!"RESERVATION".equals(resRequestDto.getIntent()))
            errors.add("환전에 관련된 요청만 가능합니다.");

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