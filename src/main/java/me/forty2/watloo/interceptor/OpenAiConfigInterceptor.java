package me.forty2.watloo.interceptor;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class OpenAiConfigInterceptor {

    @Value("${openai.api.key}")
    private String apiKey;

    @Bean
    RequestInterceptor openAiRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Authorization", "Bearer " + apiKey);
            requestTemplate.header("Content-Type", "application/json");
        };
    }

}
