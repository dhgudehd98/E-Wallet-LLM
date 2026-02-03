package com.sh.ewalletllm.llmclient.dto.gpt.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GptRequestCompletionDto {

    private GptMessageRole role;
    private String content;
}