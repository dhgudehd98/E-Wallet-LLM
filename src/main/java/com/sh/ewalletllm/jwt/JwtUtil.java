package com.sh.ewalletllm.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class JwtUtil {

    public Long extractMemberId(String authHeader) {
        String token = authHeader.replace("Bearer", "").trim();
        String[] tokens = token.split("\\.");

        if (tokens.length != 3) {
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.");
        }

        String payload = new String(Base64.getUrlDecoder().decode(tokens[1]));

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(payload);
            return jsonNode.get("memberId").asLong();
        } catch (Exception e) {
            throw new IllegalArgumentException("memberId 추출 실패 : " + e.getMessage());
        }
    }
}