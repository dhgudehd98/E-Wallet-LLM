package com.sh.ewalletllm.llmclient.service;

import com.sh.ewalletllm.api.service.AppClientService;
import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;
import com.sh.ewalletllm.reservation.dto.ResRequestDto;
import com.sh.ewalletllm.reservation.dto.ValidationResult;
import com.sh.ewalletllm.userChat.dto.UserChatRequestDto;
import com.sh.ewalletllm.userChat.dto.UserChatResponseDto;
import com.sh.ewalletllm.userChat.utils.ChatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmReservationService {

    private final AppClientService appClientService;
    private final Map<LlmType, LlmWebClientService> llmWebClientServiceMap;
    private final ChatUtil chatUtil;
    /**
     * OPEN AI 통신 구조
     * 1. 사용자 요청에 맞게 AI 요청 -> AI 응답은 -> JSON 형태로 변환
     * 2. 요청 받은 데이터 유효성 검사 확인(필수값이 정상적으로 들어가있는지 확인하기)
     *   - 성공 시 , 3번으로
     *   - 실패 시 , 필요 값 응답 메세지로 전송
     * 3. 유효성 검사 통과 했으면 실제로 APP에서 환전 예약 진행
     *  - 환전 예약 성공 / 실패 여부에 따라서 메세지 전송 = 실제로 GPT 응답
     */

    public Flux<UserChatResponseDto> getReservationCommand(UserChatRequestDto chatRequestDto, String authHeader) {
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
                                    Flux.just(
                                            new UserChatResponseDto(reservationResult.getMsg() + "\n"),
                                            new UserChatResponseDto(reservationResult.getData())
                                    )
                            );

                });

    }

    // OPEN AI -> 사용자가 입력한 요청 값 JSON으로 데이터 전달 요청
    private Mono<ResRequestDto> requestCommandJson(UserChatRequestDto userChatRequestDto) {
        String systemPrompt = reservationBuildSystemPrompt(userChatRequestDto.getRequest());
        LlmModel llmModel = userChatRequestDto.getLlmModel();

        LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(userChatRequestDto, systemPrompt);
        LlmWebClientService llmWebClientService = llmWebClientServiceMap.get(llmModel.getLlmType());
        return llmWebClientService.getCommandResRequestDto(llmChatRequestDto)
                .map(response -> chatUtil.parseJsonReservationDto(response.getLlmResponse()));
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

        if (resRequestDto.getStartDate() == null) {
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusMonths(6);

            resRequestDto.setStartDate(start);
            resRequestDto.setEndDate(end);
        }

        if (resRequestDto.getCurrencyKind() == null)
            errors.add("기준 통화가 없습니다.");

        if (resRequestDto.getCurrencyKind() == null) {
            errors.add("교환할 통화값이 존재하지 않습니다.");
        }
        if ( !resRequestDto.getCurrencyKind().equals("JPY") && !resRequestDto.getCurrencyKind().equals("USD") && !resRequestDto.getCurrencyKind().equals("EUR")){
            errors.add("입력하신 통화값으로는 환전 예약이 불가능합니다. 현재 환전예약은 JPY , USD, EUR만 환전 예약 가능합니다.");
        }
        if (resRequestDto.getInputExchangeMoney() == null)
            errors.add("예약하고 싶은 통화의 교환 금액을 입력해주세요.");

        if (errors.isEmpty()) {
            return ValidationResult.success(resRequestDto);
        }

        return ValidationResult.fail(errors);

    }

    private String reservationBuildSystemPrompt(String userRequest) {
        String commandSystemPrompt = String.format("""
                       너는 환전과 관련된 작업을 실행해주는 AI야.
                       사용자의 요청은 "%s" 이고, 
                       그리고 실제로 데이터를 반환할 때는 JSON 형식으로 반환해줘 
                       환전 예약을 요청 할 때 아래 JSON FORMAT 형태로 응답해줘.
                       변수 설명 해줄게
                       currencyKind : 사용자가 예약하고자하는 환율의 통화 
                       exchangeKind : 사용자가 지불 통화 , 없으면 KRW로 고정 
                       reservationRate : 사용자가 예약하고 싶은 환율 값 
                       inputExchangeMoney : 사용자가 예약하고자 하는 환율의 값 
                       start_date : 사용자가 예약하고싶은 시작일 
                       end_date : 예약을 걸어두고 싶은 마감일 (최대 6개월까지)
                       
                       시작일과 마감일은 항상 "yyyy-mm-dd"의 형태로 값을 입력해주고, 사용자가 시작일과 마감일에 대해서 입력하지 않았다면
                       너가 스스로 날짜 계산하지말고 반드시 null 값으로 반환해서 전달해줘 
                       예를 들어서 , 나 10USD 1400원으로 예약 해줘 하면 currencyKind에 USD , inputExchangeMoney : 10 , exchangeKind는 값이 설정 되어 있지 않으면 KRW 값으로 고정해주고, 값이 있으면 요청한 값으로 해줘 
                       {
                           intent : RESERVATION
                           currencyKind : "", 
                           exchangeKind : "",
                           inputExchangeMoney : ,
                           reservationRate : ,
                           start_date : "yyyy-mm-dd", 
                           end_date : "yyyy-mm-dd"
                           
                       }         
                       """, userRequest);

        return commandSystemPrompt;
    }

}