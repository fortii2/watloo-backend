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
    private String name;
    private String location;
    private String prof;
    private String beginTime;
    private String endTime;
}