package me.forty2.watloo.service;

import lombok.extern.slf4j.Slf4j;
import me.forty2.watloo.dto.OpenAiRequest;
import me.forty2.watloo.dto.OpenAiResponse;
import me.forty2.watloo.feign.OpenAiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class CommandCorrectionService {

    private static final Set<String> KNOWN_COMMANDS = Set.of(
            "/start",
            "/help",
            "/pick",
            "/add_course",
            "/restaurant_reviews"
    );

    private static final String SYSTEM_PROMPT =
            "You are a command spell-checker for a Telegram bot. Your job is to correct user input.\n" +
            "\n" +
            "The valid commands are:\n" +
            "/start\n" +
            "/help\n" +
            "/pick <options separated by spaces>\n" +
            "/add_course\n" +
            "/restaurant_reviews\n" +
            "\n" +
            "Rules:\n" +
            "1. If the input is already a valid command or starts with a valid command prefix, return it UNCHANGED.\n" +
            "2. If the command part has a typo or spelling error (e.g. \"/stars\" -> \"/start\", \"/halp\" -> \"/help\"), correct ONLY the command part and keep the arguments unchanged.\n" +
            "3. Fix formatting issues: date tokens like \"Jan18\" should become \"Jan 18\", \"jan18\" -> \"Jan 18\", etc.\n" +
            "4. Return ONLY the corrected input string. No explanations, no extra text, no punctuation outside the command.\n" +
            "5. If the input is completely unrecognizable as any known command, return it unchanged.";

    @Autowired
    private OpenAiClient openAiClient;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    public String check(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return messageText;
        }

        if (isExactMatch(messageText)) {
            return messageText;
        }

        try {
            OpenAiRequest request = OpenAiRequest.builder()
                    .model(model)
                    .messages(List.of(
                            OpenAiRequest.Message.builder()
                                    .role("system")
                                    .content(SYSTEM_PROMPT)
                                    .build(),
                            OpenAiRequest.Message.builder()
                                    .role("user")
                                    .content(messageText)
                                    .build()
                    ))
                    .build();

            OpenAiResponse response = openAiClient.chatCompletion(request);
            String corrected = sanitize(response.getChoices().get(0).getMessage().getContent());
            log.info("Command correction: [{}] -> [{}]", messageText, corrected);
            return corrected;
        } catch (Exception e) {
            log.error("Command correction failed, returning original input. Error: {}", e.getMessage());
            return messageText;
        }
    }

    private String sanitize(String raw) {
        if (raw == null) return "";
        String result = raw.trim().split("\n")[0].trim();
        if (result.startsWith("`") && result.endsWith("`")) {
            result = result.substring(1, result.length() - 1);
        }
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }

    private boolean isExactMatch(String messageText) {
        return KNOWN_COMMANDS.contains(messageText);
    }
}
