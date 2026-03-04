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

    BotUser botUser = userService.getOne(user);

    botUser.setUserState(UserState.AWAITING_VIEW_REVIEWS_OPTION);
    userService.save(botUser);

    return SendMessage.builder()
            .chatId(chatId)
            .text("View reviews: please choose one option")
            .replyMarkup(getViewReviewsKeyboard())
            .build();
}

private static ReplyKeyboardMarkup getViewReviewsKeyboard() {
    List<KeyboardRow> keyboard = new ArrayList<>();

    KeyboardRow row1 = new KeyboardRow();
    row1.add("Rank");
    row1.add("Search");
    keyboard.add(row1);

    ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(keyboard);
    markup.setOneTimeKeyboard(true);
    markup.setResizeKeyboard(true);
    return markup;
}

/**
 * 在你的 update/message handler 里：
 * - 当用户处于 AWAITING_VIEW_REVIEWS_OPTION 时，把输入交给这个方法
 */
public SendMessage handleViewReviewsInput(Long chatId, String messageText, User user) {
    String input = messageText == null ? "" : messageText.trim();

    if ("Rank".equalsIgnoreCase(input)) {
        return viewRank(chatId, user);
    }

    if ("Search".equalsIgnoreCase(input)) {
        // 进入搜索流程：下一条消息让用户输入关键词（餐厅名/邮编等）
        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_REVIEW_SEARCH_QUERY);
        userService.save(botUser);

        return msg(chatId, "Please enter keywords to search reviews (e.g., restaurant name or postal code):");
        // 或者：如果你想直接给搜索键盘，也可以 return viewSearch(chatId, user);
    }

    return msg(chatId, "Please choose 'Rank' or 'Search' from the keyboard.");
}

private SendMessage viewRank(Long chatId, User user) {

    List<Restaurant> restaurants = restaurantRepository.findTopRankedRestaurants();

    if (restaurants.isEmpty()) {
        return msg(chatId, "No restaurants with reviews yet.");
    }

    StringBuilder sb = new StringBuilder();
    sb.append("🏆 Top Rated Restaurants\n\n");

    int rank = 1;

    for (Restaurant r : restaurants) {
        if (rank > 20) break;

        sb.append(rank)
          .append(". ")
          .append(r.getName());

        if (r.getPostCode() != null) {
            sb.append(" (").append(r.getPostCode()).append(")");
        }

        sb.append("\n")
          .append("Rating: ")
          .append(String.format("%.2f", r.getAvgRating()))
          .append(" | Reviews: ")
          .append(r.getReviewCount())
          .append("\n\n");

        rank++;
    }

    return msg(chatId, sb.toString());
}

public SendMessage viewSearch(Long chatId, String messageText, User user) {
    String q = messageText == null ? "" : messageText.trim();
    if (q.isEmpty()) {
        return msg(chatId, "Please enter restaurant name or postal code.");
    }

    // 1) 搜餐厅：按名字 + 按邮编，合并去重
    List<Restaurant> byName = restaurantRepository
            .findTop10ByNameContainingIgnoreCaseOrderByAvgRatingDescReviewCountDesc(q);
    List<Restaurant> byPost = restaurantRepository
            .findTop10ByPostCodeContainingIgnoreCaseOrderByAvgRatingDescReviewCountDesc(q);

    Map<Long, Restaurant> merged = new java.util.LinkedHashMap<>();
    for (Restaurant r : byName) merged.put(r.getRestaurantId(), r);
    for (Restaurant r : byPost) merged.put(r.getRestaurantId(), r);

    List<Restaurant> results = new ArrayList<>(merged.values());
    if (results.isEmpty()) {
        return msg(chatId, "No restaurants found for: " + q);
    }

    // 2) 输出：最多显示 5 家，防止 Telegram 超长
    int limit = Math.min(5, results.size());
    StringBuilder sb = new StringBuilder();
    sb.append("Search results for: ").append(q).append("\n\n");

    for (int i = 0; i < limit; i++) {
        Restaurant r = results.get(i);

        double avg = r.getAvgRating() == null ? 0.0 : r.getAvgRating();
        int cnt = r.getReviewCount() == null ? 0 : r.getReviewCount();

        sb.append(i + 1).append(". ").append(nullSafe(r.getName()));
        if (r.getPostCode() != null && !r.getPostCode().isBlank()) {
            sb.append(" (").append(r.getPostCode()).append(")");
        }
        sb.append("\n")
          .append("⭐ ").append(String.format("%.2f", avg))
          .append("   •   ").append(cnt).append(" review(s)\n");

        // 3) Top3 推荐菜
        List<RecommendedDish> recs = recommendedDishRepository.findByRestaurant(r);
        List<Map.Entry<String, Long>> topRec = top3DishCounts(recs);

        if (topRec.isEmpty()) {
            sb.append("👍 Top recommended: (none yet)\n");
        } else {
            sb.append("👍 Top recommended:\n");
            for (var e : topRec) {
                sb.append("   - ").append(e.getKey())
                  .append(" (").append(e.getValue()).append(")\n");
            }
        }

        // 4) Top3 不推荐菜
        List<NotRecommendedDish> nrecs = notRecommendedDishRepository.findByRestaurant(r);
        List<Map.Entry<String, Long>> topNRec = top3DishCountsNot(nrecs);

        if (topNRec.isEmpty()) {
            sb.append("👎 Top not recommended: (none yet)\n");
        } else {
            sb.append("👎 Top not recommended:\n");
            for (var e : topNRec) {
                sb.append("   - ").append(e.getKey())
                  .append(" (").append(e.getValue()).append(")\n");
            }
        }

        sb.append("\n");
    }

    return msg(chatId, sb.toString());
}

/** null -> "(unknown)" 之类你也可以换成空串 */
private String nullSafe(String s) {
    return (s == null || s.isBlank()) ? "(unknown)" : s;
}

/** RecommendedDish 列表 -> dishName 计数 Top3 */
private List<Map.Entry<String, Long>> top3DishCounts(List<RecommendedDish> rows) {
    if (rows == null || rows.isEmpty()) return List.of();

    Map<String, Long> counts = rows.stream()
            .map(rd -> rd.getDish() == null ? null : rd.getDish().getDishName())
            .filter(name -> name != null && !name.isBlank())
            .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

    return counts.entrySet().stream()
            .sorted((a, b) -> {
                int c = Long.compare(b.getValue(), a.getValue()); // count desc
                if (c != 0) return c;
                return a.getKey().compareToIgnoreCase(b.getKey()); // name asc
            })
            .limit(3)
            .toList();
}

/** NotRecommendedDish 列表 -> dishName 计数 Top3 */
private List<Map.Entry<String, Long>> top3DishCountsNot(List<NotRecommendedDish> rows) {
    if (rows == null || rows.isEmpty()) return List.of();

    Map<String, Long> counts = rows.stream()
            .map(nd -> nd.getDish() == null ? null : nd.getDish().getDishName())
            .filter(name -> name != null && !name.isBlank())
            .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

    return counts.entrySet().stream()
            .sorted((a, b) -> {
                int c = Long.compare(b.getValue(), a.getValue());
                if (c != 0) return c;
                return a.getKey().compareToIgnoreCase(b.getKey());
            })
            .limit(3)
            .toList();
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

public SendMessage handleReview(Long chatId, String messageText, User user) {
        if ("Submit a review".equals(messageText)) {
            return submitReview(chatId, user);

        } else if ("View reviews".equals(messageText)) {
            return viewReviews(chatId, user);

        } else {
            return handleSubmitInput(chatId, messageText, user);
        }
    }
}
