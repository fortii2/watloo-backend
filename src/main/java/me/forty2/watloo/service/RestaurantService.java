package me.forty2.watloo.service;

import me.forty2.watloo.entity.BotUser;
import me.forty2.watloo.entity.NotRecommendedDish;
import me.forty2.watloo.entity.RecommendedDish;
import me.forty2.watloo.entity.Restaurant;
import me.forty2.watloo.entity.Review;
import me.forty2.watloo.enums.UserState;
import me.forty2.watloo.entity.dish;


import me.forty2.watloo.repository.UserRepository;
import me.forty2.watloo.repository.RestaurantRepository;
import me.forty2.watloo.repository.ReviewRepository;
import me.forty2.watloo.repository.DishRepository;
import me.forty2.watloo.repository.RecommendedDishRepository;
import me.forty2.watloo.repository.NotRecommendedDishRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
@Service
public class RestaurantService {
    @Autowired
    private UserService userService;

    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private DishRepository dishRepository;
    @Autowired private RecommendedDishRepository recommendedDishRepository;
    @Autowired private NotRecommendedDishRepository notRecommendedDishRepository;
    @Autowired private UserRepository userRepository;
    

    private final Map<Long, ReviewDraft> drafts = new ConcurrentHashMap<>();

    private SendMessage msg(Long chatId, String text) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
    }

    private Integer parseRating(String input) {
        try {
            int v = Integer.parseInt(input);
            return (v >= 1 && v <= 5) ? v : null;
        } catch (Exception e) {
            return null;
        }
    }
    private enum Step {
        RESTAURANT_NAME,
        POST_CODE,
        RATING,
        DESCRIPTION,
        RECOMMENDED,
        NOT_RECOMMENDED
    }

    private static class ReviewDraft {
        Step step = Step.RESTAURANT_NAME;
        String restaurantName;
        String postCode;
        Integer rating;
        String description;
        String recommended;
        String notRecommended;
    }

    public SendMessage reviews(Long chatId, User user) {
        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_RESTAURANT_REVIEW);
        userService.save(botUser);
        return functionSelection(chatId, user);
    }

    private SendMessage functionSelection(Long chatId, User user) {
        return SendMessage.builder()
                .chatId(chatId)
                .text("Please select one function")
                .replyMarkup(getReplyKeyboardMarkup())
                .build();
    }
    
    private static ReplyKeyboardMarkup getReplyKeyboardMarkup() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Submit a review");
        row1.add("View reviews");
        keyboard.add(row1);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        return replyKeyboardMarkup;
    }

    public SendMessage submitReview(Long chatId, User user) {
                drafts.put(user.getId(), new ReviewDraft());
                return msg(chatId, "Please enter the restaurant name (string):");
    }

    public SendMessage viewReviews(Long chatId, User user) {
        return SendMessage.builder()
                .chatId(chatId)
                .text("Here are the reviews")
                .build();
    }

    public SendMessage handleSubmitInput(Long chatId, String messageText, User user) {
                ReviewDraft d = drafts.get(user.getId());
        if (d == null) {
            // 用户没进入 submit 流程，却在输入普通文本
            return msg(chatId, "Please choose an option from the keyboard.");
        }

        String input = (messageText == null) ? "" : messageText.trim();

        switch (d.step) {

            case RESTAURANT_NAME -> {
                if (input.isEmpty()) {
                    return msg(chatId, "Restaurant name cannot be empty. Please enter the restaurant name:");
                }
                d.restaurantName = input;
                d.step = Step.POST_CODE;
                return msg(chatId, "Please enter the postal code (string):");
            }

            case POST_CODE -> {
                if (input.isEmpty()) {
                    return msg(chatId, "Postal code cannot be empty. Please enter the postal code:");
                }
                d.postCode = input;
                d.step = Step.RATING;
                return msg(chatId, "Please enter rating (integer 1-5):");
            }

            case RATING -> {
                Integer rating = parseRating(input);
                if (rating == null) {
                    return msg(chatId, "Invalid rating. Please enter an integer between 1 and 5:");
                }
                d.rating = rating;
                d.step = Step.DESCRIPTION;
                return msg(chatId, "Please enter description (string). Type 'skip' to leave it empty:");
            }

            case DESCRIPTION -> {
                if ("skip".equalsIgnoreCase(input) || input.isEmpty()) {
                    d.description = null;
                } else {
                    d.description = input;
                }
                d.step = Step.RECOMMENDED;
                return msg(chatId, "Please enter recommended dishes (comma-separated) or type 'none':");
            }

            case RECOMMENDED -> {
                if ("none".equalsIgnoreCase(input) || input.isEmpty()) {
                    d.recommended = null;
                } else {
                    d.recommended = input;
                }
                d.step = Step.NOT_RECOMMENDED;
                return msg(chatId, "Please enter NOT recommended dishes (comma-separated) or type 'none':");
            }

            case NOT_RECOMMENDED -> {
                if ("none".equalsIgnoreCase(input) || input.isEmpty()) {
                    d.notRecommended = null;
                } else {
                    d.notRecommended = input;
                }
                updateDb(
                    d.restaurantName,
                    d.postCode,
                    d.rating,
                    d.description,
                    d.recommended,
                    d.notRecommended,
                    user.getId()
                ); 
                // 清掉草稿
                drafts.remove(user.getId());

                return msg(chatId, "Thanks for review");
            }
        }

        // 理论上到不了这里
        return msg(chatId, "Please continue.");
    }

private void updateDb(
        String restaurantName,
        String postCode,
        Integer rating,
        String description,
        String recommended,
        String notRecommended,
        Long userId
) {
    Restaurant restaurant = restaurantRepository
            .findByNameAndPostCode(restaurantName, postCode)
            .orElseGet(() -> {
                Restaurant r = new Restaurant();
                r.setName(restaurantName);
                r.setPostCode(postCode);
                r.setReviewCount(0);
                r.setAvgRating(0.0);
                return restaurantRepository.save(r);
            });

    BotUser user = userRepository.findById(userId).orElseThrow();

    Review review = new Review();
    review.setRestaurant(restaurant);
    review.setUser(user);
    review.setRating(rating);
    review.setDescription(description);
    reviewRepository.save(review);

    handleDishes(recommended, restaurant, review, true);
    handleDishes(notRecommended, restaurant, review, false);

    int oldCount = restaurant.getReviewCount() == null ? 0 : restaurant.getReviewCount();
    double oldAvg = restaurant.getAvgRating() == null ? 0.0 : restaurant.getAvgRating();

    int newCount = oldCount + 1;
    double newAvg = (oldAvg * oldCount + rating) / newCount;

    restaurant.setReviewCount(newCount);
    restaurant.setAvgRating(newAvg);

    restaurantRepository.save(restaurant);
}

private void handleDishes(
        String dishInput,
        Restaurant restaurant,
        Review review,
        boolean isRecommended
    ) {
        if (dishInput == null || dishInput.isBlank()) return;

        // split + trim + 去空 + 去重
        Set<String> dishNames = Arrays.stream(dishInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String name : dishNames) {
            dish dish = dishRepository
                    .findByRestaurantAndDishName(restaurant, name)
                    .orElseGet(() -> {
                        dish d = new dish();
                        d.setRestaurant(restaurant);
                        d.setDishName(name);
                        d.setPrice(BigDecimal.ZERO);
                        d.setRecommendCount(0);
                        d.setNotRecommendCount(0);
                        return dishRepository.save(d);
                    });

            if (isRecommended) {
                RecommendedDish rd = new RecommendedDish();
                rd.setReview(review);
                rd.setDish(dish);
                recommendedDishRepository.save(rd);

                int rec = dish.getRecommendCount() == null ? 0 : dish.getRecommendCount();
                dish.setRecommendCount(rec + 1);
            } else {
                NotRecommendedDish nd = new NotRecommendedDish();
                nd.setReview(review);
                nd.setDish(dish);
                notRecommendedDishRepository.save(nd);

                int nrec = dish.getNotRecommendCount() == null ? 0 : dish.getNotRecommendCount();
                dish.setNotRecommendCount(nrec + 1);
            }

            dishRepository.save(dish);
        }
    }

    private SendMessage handleReview(Long chatId, String messageText, User user) {
        if ("Submit a review".equals(messageText)) {
            return submitReview(chatId, user);

        } else if ("View reviews".equals(messageText)) {
            return viewReviews(chatId, user);

        } else {
            return handleSubmitInput(chatId, messageText, user);
        }
    }
}
