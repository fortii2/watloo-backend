package me.forty2.watloo.controller;

import me.forty2.watloo.dto.CourseTableDTO;
import me.forty2.watloo.entity.BotUser;
import me.forty2.watloo.service.CourseService;
import me.forty2.watloo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CourseTableController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @GetMapping("/courses")
    public ResponseEntity<List<CourseTableDTO>> getCourses(
            @RequestHeader("X-Telegram-User-Id") Long telegramUserId) {

        BotUser botUser = userService.getByTelegramId(telegramUserId);

        if (botUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<CourseTableDTO> courses = courseService.getUserCoursesForApi(botUser);

        return ResponseEntity.ok(courses);
    }
}
