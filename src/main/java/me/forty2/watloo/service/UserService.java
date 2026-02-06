package me.forty2.watloo.service;

import lombok.RequiredArgsConstructor;
import me.forty2.watloo.entity.BotUser;
import me.forty2.watloo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void syncUser(User user) {
        userRepository.save(getOne(user));
    }

    public BotUser getOne(User user) {
        return userRepository.findById(user.getId())
                .orElseGet(() -> BotUser.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .username(user.getUserName())
                        .languageCode(user.getLanguageCode())
                        .build());
    }

    public void save(BotUser botUser) {
        userRepository.save(botUser);
    }

    public BotUser getByTelegramId(Long telegramUserId) {
        return userRepository.findById(telegramUserId).orElseThrow();
    }
}
