package me.forty2.watloo.controller;

import lombok.extern.slf4j.Slf4j;
import me.forty2.watloo.service.MessageRouter;
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

    private final MessageRouter messageRouter;


    public WatlooBot(@Value("${telegram.bot.token}") String token,
                     UserService userService,
                     MessageRouter messageRouter) {

        this.token = token;
        this.telegramClient = new OkHttpTelegramClient(getBotToken());
        this.userService = userService;
        this.messageRouter = messageRouter;
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
            Long chatId = update.getMessage().getChatId();
            try {
                userService.syncUser(update.getMessage().getFrom());
                SendMessage handled = messageRouter.router(
                        chatId,
                        update.getMessage().getText(),
                        update.getMessage().getFrom()
                );
                if (handled != null) {
                    telegramClient.execute(handled);
                }
            } catch (TelegramApiException e) {
                log.error("Telegram API error: {}", e.getMessage());
                sendText(chatId, "Sorry, failed to send the reply. Please try again.");
            } catch (Exception e) {
                log.error("Error handling message", e);
                sendText(chatId, "Something went wrong. Please try again.");
            }
        }
    }

    private void sendText(Long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException ex) {
            log.error("Failed to send error message: {}", ex.getMessage());
        }
    }
}