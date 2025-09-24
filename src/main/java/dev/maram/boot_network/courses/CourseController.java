package dev.maram.boot_network.courses;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("courses")
public class CourseController {

    //private final CourseService courseService;

//    public CourseController(final CourseService courseService) {
//        this.courseService = courseService;
//    }

    @GetMapping
    public ResponseEntity<?> test() {
        return ResponseEntity.ok("hello");
    }

    @GetMapping("test")
    public ResponseEntity<String> courses(String c) {
        return ResponseEntity.ok("testing courses");
        //return courseService.getCourse(c);
    }
}
