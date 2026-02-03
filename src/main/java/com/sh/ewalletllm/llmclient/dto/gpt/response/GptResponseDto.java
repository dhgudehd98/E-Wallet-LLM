package com.sh.ewalletllm.llmclient.dto.gpt.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GptResponseDto {

    private List<GptChoice> choices;
    public GptChoice getSingleChoice() {
        return choices.stream().findFirst().orElseThrow();
    }
}