package me.forty2.watloo.controller;

import me.forty2.watloo.dto.ApiErrorResponse;
import me.forty2.watloo.dto.GetCoursesResponse;
import me.forty2.watloo.entity.BotUser;
import me.forty2.watloo.service.CourseService;
import me.forty2.watloo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"", "/api"})
@CrossOrigin(origins = "*")
public class CourseTableController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    @GetMapping("/courses")
    public ResponseEntity<?> getCourses(
            @RequestParam(value = "userId", required = false) Long telegramUserId,
            @RequestParam(value = "view", defaultValue = "week") String view,
            @RequestParam(value = "date", required = false) String date) {

        if (telegramUserId == null) {
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "missing userId parameter");
        }

        BotUser botUser = userService.getByTelegramId(telegramUserId);

        if (botUser == null) {
            return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "user is not authorized");
        }

        try {
            GetCoursesResponse response = courseService.getUserCoursesForApi(botUser, view, date);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage() == null ? "invalid request" : ex.getMessage();
            String[] parts = message.split(":", 2);
            String code = parts.length > 1 ? parts[0] : "BAD_REQUEST";
            String detail = parts.length > 1 ? parts[1] : message;
            return error(HttpStatus.BAD_REQUEST, code, detail);
        } catch (Exception ex) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "server error");
        }
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.builder()
                        .code(code)
                        .message(message)
                        .build());
    }
}
