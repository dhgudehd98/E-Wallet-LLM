package com.sh.ewalletllm.llmclient;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LlmModel {
    GPT_4o("gpt-4o" ,LlmType.GPT),
    GEMINI_2_0_FLASH("gemini-2.0-flash", LlmType.GEMINI);

    private String code;
    private LlmType llmType;

    @JsonValue
    @Override
    public String toString(){
        return code;
    }
}