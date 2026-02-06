package me.forty2.watloo.service;

import me.forty2.watloo.entity.BotUser;
import me.forty2.watloo.enums.UserState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
public class MessageRouter {

    @Autowired
    private UtilityService utilityService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;


    public SendMessage router(Long chatId, String messageText, User user) {

        if (messageText.startsWith("/")) {
            return commandHandler(chatId, messageText, user);
        } else {
            return stateHandler(chatId, messageText, user);
        }

    }

    private SendMessage stateHandler(Long chatId, String messageText, User user) {
        BotUser botUser = userService.getOne(user);
        UserState userState = botUser.getUserState();

        return switch (userState) {
            case AWAITING_TERM_SELECTION -> courseService.bindTerm(chatId, messageText, user);
            case AWAITING_COURSE_NAME_INPUT -> courseService.handleCourseName(chatId, messageText, user);
            case AWAITING_LOCATION_INPUT -> courseService.handleLocation(chatId, messageText, user);
            case AWAITING_DAY_SELECTION -> courseService.handleDay(chatId, messageText, user);
            case AWAITING_TIME_INPUT -> courseService.handleTime(chatId, messageText, user);
            case AWAITING_PROF_INPUT -> courseService.saveCourse(chatId, messageText, user);
        };
    }

    private SendMessage commandHandler(Long chatId, String messageText, User user) {
        if (messageText.startsWith("/start")) {

            String response = utilityService.startHandler();
            return SendMessage
                    .builder()
                    .chatId(chatId)
                    .text(response)
                    .build();

        } else if (messageText.startsWith("/help")) {
            String response = utilityService.helpHandler();
            return SendMessage
                    .builder()
                    .chatId(chatId)
                    .text(response)
                    .build();

        } else if (messageText.startsWith("/pick")) {
            String response = utilityService.pickHandler(messageText);
            return SendMessage
                    .builder()
                    .chatId(chatId)
                    .text(response)
                    .build();

        } else if (messageText.startsWith("/add_course")) {
            return courseService.registerCourse(chatId, user);

        } else {
            String response = utilityService.defaultHandler();
            return SendMessage
                    .builder()
                    .chatId(chatId)
                    .text(response)
                    .build();
        }
    }
}
