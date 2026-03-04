package me.forty2.watloo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRegisterDTO {
    private Long telegramUserId;
    private String name;
    private String condition;
    private String price;
}
