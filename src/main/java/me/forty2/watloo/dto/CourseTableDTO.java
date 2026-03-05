package me.forty2.watloo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseTableDTO {
    private String id;
    private String name;
    private String location;
    private String professor;
    private Integer dayOfWeek;
    private String date;
    private String beginTime;
    private String endTime;
}