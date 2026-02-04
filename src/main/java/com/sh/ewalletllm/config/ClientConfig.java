package com.sh.ewalletllm.config;

import com.sh.ewalletllm.llmclient.LlmType;
import com.sh.ewalletllm.llmclient.service.LlmWebClientService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ClientConfig {
    @Bean
    public Map<LlmType, LlmWebClientService> getLlmWebClientServiceMap(List<LlmWebClientService> llmWebClientServiceList) {
        // llmType 을 통해서 어떤 OPEN API를 사용할지 선택해주는 Configuration
        // llmTYpe : Gemini -> GeminiWebClientService 사용 , llmType : gpt -> GptWebClientWebService 사용
        return llmWebClientServiceList.stream().collect(Collectors.toMap(llmWebClientService -> llmWebClientService.getLlmType(), Function.identity()));
    }
}