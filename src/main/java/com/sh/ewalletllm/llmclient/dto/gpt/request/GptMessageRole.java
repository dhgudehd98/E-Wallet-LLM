package com.sh.ewalletllm.llmclient.dto.gpt.request;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GptMessageRole {

    SYSTEM, // System Prompt
    USER, // User Input
    ASSISTANT // AI 응답
    ;

    @JsonValue
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}