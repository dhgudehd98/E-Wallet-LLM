package com.sh.ewalletllm.userChat.utils;

import com.sh.ewalletllm.llmclient.dto.Apply.UserApplyInfoDto;
import com.sh.ewalletllm.llmclient.dto.LlmChatResponseDto;
import com.sh.ewalletllm.llmclient.dto.reservation.ResRequestDto;
import com.sh.ewalletllm.userChat.dto.IntentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;


@Slf4j
@Component
@RequiredArgsConstructor
public class ChatUtil {

    private final ObjectMapper objectMapper;

    //요청한 Intent Dto에 대한 값 -> JSON 형식으로 변환
    public IntentDto parseJsonIntentDto(String llmResponse) {
        log.info("[GPT RESPONSE DATA] : " + llmResponse);
        String jsonString = extractJsonString(llmResponse);
        try {
            return objectMapper.readValue(jsonString, IntentDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("[LLM INTENT Parsing error] : IntentDto Parsing Error");
        }
    }

    // 요청한 Reservation DTO에 대한 값 -> JSON 형식으로 변환
    public ResRequestDto parseJsonReservationDto(String llmResponse) {
        String jsonString = extractJsonString(llmResponse);
        try {
            return  objectMapper.readValue(jsonString, ResRequestDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("[LLM Reservation Parssing error] : String -> ResRequestDto Parsing Failed");
        }
    }

    public  String extractJsonString(String content) {
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


    public UserApplyInfoDto parseUserApplyInfoDto(String llmResponse) {
        String jsonString = extractJsonString(llmResponse);
        try {
            return  objectMapper.readValue(jsonString, UserApplyInfoDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("[LLM Reservation Parssing error] : String -> ResRequestDto Parsing Failed");
        }
    }
}