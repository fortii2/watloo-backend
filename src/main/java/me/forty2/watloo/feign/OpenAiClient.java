package me.forty2.watloo.feign;

import me.forty2.watloo.dto.OpenAiRequest;
import me.forty2.watloo.dto.OpenAiResponse;
import me.forty2.watloo.interceptor.OpenAiConfigInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "openAiClient",
        url = "${openai.api.url:https://api.openai.com/v1}",
        configuration = OpenAiConfigInterceptor.class)
public interface OpenAiClient {

    @PostMapping("/chat/completions")
    OpenAiResponse chatCompletion(@RequestBody OpenAiRequest request);
}
