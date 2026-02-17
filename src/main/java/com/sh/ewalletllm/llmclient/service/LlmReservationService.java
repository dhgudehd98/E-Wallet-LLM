package com.sh.ewalletllm.llmclient.service;

import com.sh.ewalletllm.api.service.AppClientService;
import com.sh.ewalletllm.chathistory.service.UserChatHistoryService;
import com.sh.ewalletllm.llmclient.LlmModel;
import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.dto.LlmChatRequestDto;

import com.sh.ewalletllm.llmclient.dto.reservation.ResRequestDto;
import com.sh.ewalletllm.llmclient.dto.reservation.ReservationValidationResult;
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
    private final UserChatHistoryService userChatHistoryService;


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
        return requestCommandJson(chatRequestDto)
                .flatMapMany(response -> {
                    // 자연어 응답이면 그대로 사용자에게 전달
                    if (response instanceof String) {
                        return Flux.just(new UserChatResponseDto((String) response));
                    }

                    // JSON 파싱 성공이면 유효성 검사 후 예약 실행
                    ResRequestDto resRequestDto = (ResRequestDto) response;
                    ReservationValidationResult result = validateRequestDto(resRequestDto);

                    if (!result.isResult()) {
                        return Flux.fromIterable(result.getErrors())
                                .map(error -> new UserChatResponseDto(error));
                    }

                    return appClientService.appClientReservation(result.getDto(), authHeader)
                            .flatMapMany(reservationResult ->
                                    Flux.just(new UserChatResponseDto(reservationResult.getMsg() + "\n" + reservationResult.getData()))
                            );
                });

    }

    // OPEN AI -> 사용자가 입력한 요청 값 JSON으로 데이터 전달 요청
    private Mono<Object> requestCommandJson(UserChatRequestDto userChatRequestDto) {
        String systemPrompt = reservationBuildSystemPrompt(userChatRequestDto.getRequest());
        LlmModel llmModel = userChatRequestDto.getLlmModel();

        //! 여기는 나중에 authHeader에 대한 값으로 변경
        Long memberId = 102L;
        return userChatHistoryService.getRecentHistory(memberId)
                .collectList()
                .flatMap(historyList -> {
                    LlmChatRequestDto llmChatRequestDto = new LlmChatRequestDto(
                            userChatRequestDto,
                            systemPrompt,
                            historyList
                    );

                    LlmWebClientService llmWebClientService = llmWebClientServiceMap.get(llmModel.getLlmType());
                    return llmWebClientService.getCommandResRequestDto(llmChatRequestDto)
                            .map(responseDto -> {
                                ResRequestDto parsingData = chatUtil.parseJsonReservationDto(responseDto.getLlmResponse());

                                if (parsingData == null) {
                                    return (Object) responseDto.getLlmResponse();
                                }
                                return (Object) parsingData;
                            });

                });
    }

    /**
     * OPEN AI에서 만든 데이터 값 유효성 검사
     * 필수로 있어야되는 데이터 :
     * - currencyKind : 지불 통화 ,
     * - exchangeKind : 예약할 통화,
     * - inputExchangeMoney : 예약하고 싶은 환율의 필요 금액
     */
    private ReservationValidationResult validateRequestDto(ResRequestDto resRequestDto) {
        log.info("[Validation ResRequestDto] RequestDto : " + resRequestDto);

        List<String> errors = new ArrayList<>();

        if (resRequestDto.getStartDate() == null) {
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusMonths(6);

            resRequestDto.setStartDate(start);
            resRequestDto.setEndDate(end);
        }

        if (resRequestDto.getReservationRate() == null) {
            errors.add("예약 환율을 입력해주세요 !");
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
            return ReservationValidationResult.success(resRequestDto);
        }

        return ReservationValidationResult.fail(errors);

    }

    private String reservationBuildSystemPrompt(String userRequest) {
        return String.format("""
        너는 환전 예약을 실행해주는 AI야.
        
        [사용자 요청]
        "%s"
        
        [중요: 이전 대화 맥락 활용]
        - 이전 대화 히스토리가 있으면 반드시 참고해서 누락된 값을 채워줘.
        - 예시:
          user: 14USD 환전 예약해줘
          assistant: 얼마에 예약할까요?
          user: 1400원
          → currencyKind: USD, inputExchangeMoney: 14, reservationRate: 1400 으로 판단 ✅
        
        [값이 부족할 때 규칙]
        - 히스토리를 참고해도 필요한 값이 없으면 사용자에게 되물어봐.
        - 단, 되물어볼 때는 JSON이 아닌 자연어로 질문해줘.
        - 예: "환전 예약 환율을 얼마로 설정할까요?"
        
        [모든 값이 있을 때 규칙]
        - 아래 JSON 형식으로만 응답해.
        - currencyKind: 예약하고자 하는 환율의 통화
        - exchangeKind: 지불 통화, 없으면 KRW로 고정
        - reservationRate: 예약하고 싶은 환율 값
        - inputExchangeMoney: 예약하고자 하는 환전 금액
        - start_date: 예약 시작일 (yyyy-MM-dd), 사용자가 말 안하면 null
        - end_date: 예약 마감일 (yyyy-MM-dd, 최대 6개월), 사용자가 말 안하면 null
        
        {
            "intent": "RESERVATION",
            "currencyKind": "",
            "exchangeKind": "",
            "inputExchangeMoney": ,
            "reservationRate": ,
            "start_date": "yyyy-MM-dd",
            "end_date": "yyyy-MM-dd"
        }
        
        [주의]
        - 날짜는 절대 스스로 계산하지 마. 사용자가 말 안하면 반드시 null로 반환해.
        - JSON 응답 시 다른 텍스트 절대 포함하지 마.
        """, userRequest);
    }

}