package me.forty2.watloo.enums;

import lombok.Getter;

@Getter
public enum UserState {
    AWAITING_TERM_SELECTION,
    AWAITING_COURSE_NAME_INPUT,
    AWAITING_LOCATION_INPUT,
    AWAITING_DAY_SELECTION,
    AWAITING_TIME_INPUT,
    AWAITING_PROF_INPUT,
}
