package dev.maram.boot_network.courses;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CourseService {

    @RateLimiter(name="getCourseRateLimiter", fallbackMethod = "fallbackGetCourse")
    public ResponseEntity<String> getCourse(final String c) {
        return ResponseEntity.ok("the course is " + c);
    }

    public String fallbackGetCourse(Throwable throwable) {
        return "Too many requests. Please try again later.";
    }
}
