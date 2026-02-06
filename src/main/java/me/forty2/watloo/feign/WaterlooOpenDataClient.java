package me.forty2.watloo.feign;

import me.forty2.watloo.entity.CourseTable;
import me.forty2.watloo.entity.Term;
import me.forty2.watloo.interceptor.UwOpenapiConfigInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
        name = "waterlooOpenDataClient",
        url = "https://openapi.data.uwaterloo.ca/v3",
        configuration = UwOpenapiConfigInterceptor.class)
public interface WaterlooOpenDataClient {

    @GetMapping("/Terms")
    List<Term> getAllTerms();

    @GetMapping("/ClassSchedules")
    List<CourseTable> getClassScheduleByTermCodeAndCourseName();
}
