package me.forty2.watloo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRegisterDTO {
    private Long userId;

    private String termCode;

    private String courseName;

    private String location;

    private String day;

    private String time;

    private String prof;
}
