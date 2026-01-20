package me.forty2.watloo.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class MessageService {

    public SendMessage handle(Long chatId, String messageText) {
        String response = "";

        if (messageText.startsWith("/start")) {
            response = startHandler();
        } else if (messageText.startsWith("/help")) {
            response = helpHandler();
        } else if (messageText.startsWith("/pick")) {
            response = pickHandler(messageText);
        } else {
            response = defaultHandler();
        }

        return SendMessage
                .builder()
                .chatId(chatId)
                .text(response)
                .build();
    }

    private String startHandler() {
        return """
                Welcome!
                
                You can use /help to see all available commands.
                """;
    }

    private String helpHandler() {
        return """
                /pick with arguments split by whitespace for random selection
                
                For example, /pick KFC McDonald's BurgerKing
                
                /help for help documents
                """;
    }

    private String pickHandler(String msg) {
        String args = msg.substring(5).trim();
        String[] split = args.split("\\s+");

        if (args.isBlank() || split.length == 0) {
            return "Please provide at least 1 option.";
        }

        int random = ThreadLocalRandom.current().nextInt(split.length);

        return split[random];
    }

    private String defaultHandler() {
        return """
                Please input valid commands.
                
                You can use /help to see all available commands.
                """;
    }
}
