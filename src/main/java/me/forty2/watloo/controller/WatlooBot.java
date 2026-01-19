package me.forty2.watloo.controller;

import lombok.extern.slf4j.Slf4j;
import me.forty2.watloo.service.MessageService;
import me.forty2.watloo.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Controller
public class WatlooBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String token;

    private final TelegramClient telegramClient;

    private final UserService userService;

    private final MessageService messageService;


    public WatlooBot(@Value("${telegram.bot.token}") String token,
                     UserService userService,
                     MessageService messageService) {

        this.token = token;
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
        this.userService = userService;
        this.messageService = messageService;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {

            userService.syncUser(update.getMessage().getFrom());

            SendMessage handled =
                    messageService.handle(
                            update.getMessage().getChatId(),
                            update.getMessage().getText()
                    );

            try {
                telegramClient.execute(handled);
            } catch (TelegramApiException e) {
                log.error("@error: {}", e.getMessage());
            }
        }

    }
}