package me.forty2.watloo.service;

import lombok.extern.slf4j.Slf4j;
import me.forty2.watloo.entity.BotUser;
import me.forty2.watloo.enums.UserState;
import me.forty2.watloo.exception.RemoveItemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Slf4j
@Service
public class MessageRouter {

    @Autowired
    private UtilityService utilityService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private ItemService itemService;

    public SendMessage router(Long chatId, String messageText, User user) {

        if (messageText.startsWith("/")) {
            return commandHandler(chatId, messageText, user);
        } else {
            return stateHandler(chatId, messageText, user);
        }

    }

    private SendMessage stateHandler(Long chatId, String messageText, User user) {
        try {
            BotUser botUser = userService.getOne(user);
            UserState userState = botUser.getUserState();
            if (userState == null) {
                return SendMessage.builder().chatId(chatId).text(utilityService.defaultHandler()).build();
            }
            return switch (userState) {
                case AWAITING_TERM_SELECTION -> courseService.bindTerm(chatId, messageText, user);
                case AWAITING_COURSE_NAME_INPUT -> courseService.handleCourseName(chatId, messageText, user);
                case AWAITING_LOCATION_INPUT -> courseService.handleLocation(chatId, messageText, user);
                case AWAITING_DAY_SELECTION -> courseService.handleDay(chatId, messageText, user);
                case AWAITING_TIME_INPUT -> courseService.handleTime(chatId, messageText, user);
                case AWAITING_PROF_INPUT -> courseService.saveCourse(chatId, messageText, user);
                case AWAITING_RESTAURANT_REVIEW -> restaurantService.handleReview(chatId, messageText, user);
                case AWAITING_ITEM_NAME -> itemService.handleItemName(chatId, messageText, user);
                case AWAITING_ITEM_CONDITION -> itemService.handleItemCondition(chatId, messageText, user);
                case AWAITING_ITEM_PRICE -> itemService.handleItemPrice(chatId, messageText, user);
            };
        } catch (Exception e) {
            return SendMessage.builder().chatId(chatId).text("Something went wrong. Please try again.").build();
        }
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
        } else if (messageText.startsWith("/restaurant_reviews")) {
            return restaurantService.reviews(chatId, user);
        } else if (messageText.startsWith("/additem")) {
            try {
                return itemService.startAddItem(chatId, user);
            } catch (Exception e) {
                log.error("Failed to start add item for chatId={}", chatId, e);
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("Failed to start add item. Please try again.")
                        .build();
            }
        } else if (messageText.startsWith("/items")) {
            return itemService.buildItemsMessage(chatId);
        } else if (messageText.startsWith("/myitems")) {
            return itemService.buildMyItemsMessage(chatId, user.getId());
        } else if (messageText.startsWith("/removeitem")) {
            return handleRemoveItem(chatId, messageText, user);
        } else {
            String response = utilityService.defaultHandler();
            return SendMessage
                    .builder()
                    .chatId(chatId)
                    .text(response)
                    .build();
        }
    }

    private SendMessage handleRemoveItem(Long chatId, String messageText, User user) {
        String payload = messageText.substring("/removeitem".length()).trim();
        if (payload.isBlank()) {
            return SendMessage.builder().chatId(chatId).text("Item ID is required. Use /myitems to see your items and their IDs. Usage: /removeitem <id>").build();
        }
        Long itemId;
        try {
            itemId = Long.parseLong(payload);
        } catch (NumberFormatException e) {
            return SendMessage.builder().chatId(chatId).text("Invalid item ID. Usage: /removeitem <id>").build();
        }
        try {
            String response = itemService.removeItem(user.getId(), itemId);
            return SendMessage.builder().chatId(chatId).text(response).build();
        } catch (RemoveItemException e) {
            return SendMessage.builder().chatId(chatId).text("Error: " + e.getMessage()).build();
        }
    }

}
