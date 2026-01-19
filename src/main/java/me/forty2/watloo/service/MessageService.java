package me.forty2.watloo.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Service
public class MessageService {

    public SendMessage handle(Long chatId, String messageText) {
        return SendMessage
                .builder()
                .chatId(chatId)
                .text(messageText)
                .build();
    }
}
