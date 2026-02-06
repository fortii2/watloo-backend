package me.forty2.watloo.service;

import me.forty2.watloo.dto.CourseRegisterDTO;
import me.forty2.watloo.dto.CourseTableDTO;
import me.forty2.watloo.entity.BotUser;
import me.forty2.watloo.entity.CourseTable;
import me.forty2.watloo.entity.Term;
import me.forty2.watloo.enums.UserState;
import me.forty2.watloo.repository.CourseTableRepository;
import me.forty2.watloo.repository.TermRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseService {

    private final Map<Long, CourseRegisterDTO> registrationCache = new HashMap<>();
    @Autowired
    private UserService userService;
    @Autowired
    private TermRepository termRepository;
    @Autowired
    private CourseTableRepository courseTableRepository;

    private static ReplyKeyboardMarkup getDayKeyboardMarkup() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Monday");
        row1.add("Tuesday");
        row1.add("Wednesday");
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Thursday");
        row2.add("Friday");
        keyboard.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Saturday");
        row3.add("Sunday");
        keyboard.add(row3);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getReplyKeyboardMarkup(List<Term> terms) {
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(terms.get(0).getName());
        row1.add(terms.get(1).getName());
        keyboard.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(terms.get(2).getName());
        row2.add(terms.get(3).getName());
        keyboard.add(row2);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        return replyKeyboardMarkup;
    }

    public SendMessage registerCourse(Long chatId, User user) {
        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_TERM_SELECTION);
        userService.save(botUser);

        registrationCache.put(user.getId(), CourseRegisterDTO.builder()
                .userId(user.getId())
                .build());

        return allTerms(chatId, user);
    }

    public SendMessage bindTerm(Long chatId, String messageText, User user) {
        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_COURSE_NAME_INPUT);
        userService.save(botUser);

        Term term = termRepository.findByName(messageText);

        if (term == null) {
            throw new RuntimeException("term is not exist!");
        }

        CourseRegisterDTO dto = registrationCache.get(user.getId());
        dto.setTermCode(term.getTermCode());

        String response = """
                Please input course Name, Like:
                ECE 650
                """;

        return SendMessage.builder()
                .chatId(chatId)
                .text(response)
                .build();
    }

    public SendMessage handleCourseName(Long chatId, String courseName, User user) {
        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_LOCATION_INPUT);
        userService.save(botUser);

        CourseRegisterDTO dto = registrationCache.get(user.getId());
        dto.setCourseName(courseName);

        return SendMessage.builder()
                .chatId(chatId)
                .text("Please input the location (e.g., E7 3343):")
                .build();
    }

    public SendMessage handleLocation(Long chatId, String location, User user) {
        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_DAY_SELECTION);
        userService.save(botUser);

        CourseRegisterDTO dto = registrationCache.get(user.getId());
        dto.setLocation(location);

        return SendMessage.builder()
                .chatId(chatId)
                .text("Please select the day of week:")
                .replyMarkup(getDayKeyboardMarkup())
                .build();
    }

    public SendMessage handleDay(Long chatId, String day, User user) {
        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_TIME_INPUT);
        userService.save(botUser);

        CourseRegisterDTO dto = registrationCache.get(user.getId());
        dto.setDay(day);

        return SendMessage.builder()
                .chatId(chatId)
                .text("Please input the time (e.g., 10:00-11:20):")
                .build();
    }

    public SendMessage handleTime(Long chatId, String time, User user) {
        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_PROF_INPUT);
        userService.save(botUser);

        CourseRegisterDTO dto = registrationCache.get(user.getId());
        dto.setTime(time);

        return SendMessage.builder()
                .chatId(chatId)
                .text("Please input the professor's name (or send 'skip' to leave empty):")
                .build();
    }

    public SendMessage saveCourse(Long chatId, String profName, User user) {
        CourseRegisterDTO dto = registrationCache.get(user.getId());
        dto.setProf(profName.equalsIgnoreCase("skip") ? "" : profName);

        String[] parts = dto.getCourseName().split(" ");
        String subject = parts[0];
        Integer subjectNumber = Integer.parseInt(parts[1]);

        saveCourseForTerm(
                dto.getUserId(),
                subject,
                subjectNumber,
                dto.getTermCode(),
                dto.getLocation(),
                dto.getProf(),
                DayOfWeek.valueOf(dto.getDay().toUpperCase()),
                dto.getTime()
        );

        BotUser botUser = userService.getOne(user);
        botUser.setUserState(null);
        userService.save(botUser);

        registrationCache.remove(user.getId());

        return SendMessage.builder().chatId(chatId)
                .text("Course added successfully for the entire term!")
                .build();
    }


    public SendMessage allTerms(Long chatId, User user) {
        List<Term> terms = termRepository.findTop4ByTermEndDateAfterOrderByTermBeginDateAsc(LocalDateTime.now());

        BotUser botUser = userService.getOne(user);
        userService.save(botUser);

        return SendMessage.builder()
                .chatId(chatId)
                .text("Please select one term")
                .replyMarkup(getReplyKeyboardMarkup(terms))
                .build();
    }

    public void saveCourseForTerm(Long userId, String subject, Integer subjectNumber,
                                  String termCode, String location, String prof,
                                  DayOfWeek dayOfWeek, String timeRange) {

        Term term = termRepository.findById(termCode)
                .orElseThrow(() -> new RuntimeException("Term not found: " + termCode));

        LocalDate termStart = term.getTermBeginDate().toLocalDate();
        LocalDate termEnd = term.getTermEndDate().toLocalDate();

        String[] times = timeRange.split("-");
        LocalTime beginTime = LocalTime.parse(times[0].trim());
        LocalTime endTime = LocalTime.parse(times[1].trim());

        LocalDate currentDate = termStart;
        while (currentDate.getDayOfWeek() != dayOfWeek) {
            currentDate = currentDate.plusDays(1);
        }

        while (!currentDate.isAfter(termEnd)) {
            CourseTable course = CourseTable.builder()
                    .userId(userId)
                    .subject(subject)
                    .subjectNumber(subjectNumber)
                    .termCode(termCode)
                    .location(location)
                    .prof(prof)
                    .beginTime(LocalDateTime.of(currentDate, beginTime))
                    .endTime(LocalDateTime.of(currentDate, endTime))
                    .build();

            courseTableRepository.save(course);

            currentDate = currentDate.plusWeeks(1);
        }
    }

    public List<CourseTableDTO> getUserCoursesForApi(BotUser botUser) {
        List<CourseTable> allCourses = courseTableRepository.findAllByUserId(botUser.getId());
        return allCourses.stream().map(course ->
                CourseTableDTO.builder()
                        .name(course.getSubject() + " " + course.getSubjectNumber())
                        .location(course.getLocation())
                        .prof(course.getProf())
                        .beginTime(course.getBeginTime().toString())
                        .endTime(course.getEndTime().toString())
                        .build()

        ).toList();
    }
}
