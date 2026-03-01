package com.example.backend;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    /*
    In-memory data storage
    */

    private Map<String, List<Map<String,String>>> classes = new HashMap<>();

    private String validTeacher = "teacher";
    private String validPassword = "password";
    private String fakeToken = "abc123";


    /*
    Initialize fake data
    */
    public ApiController() {

        List<Map<String,String>> students = new ArrayList<>();

        students.add(createStudent("s1", "Alice"));
        students.add(createStudent("s2", "Bob"));
        students.add(createStudent("s3", "Charlie"));

        classes.put("COSC4P78", students);

    }


    private Map<String,String> createStudent(String id, String name) {

        Map<String,String> student = new HashMap<>();

        student.put("studentID", id);
        student.put("studentName", name);

        return student;

    }


    /*
    Verify class code
    */
    @PostMapping("/class/verify")
    public Map<String,Object> verifyClass(@RequestBody Map<String,String> request) {

        String classCode = request.get("classCode");

        Map<String,Object> response = new HashMap<>();

        if (classes.containsKey(classCode)) {

            response.put("success", true);
            response.put("message", "Class exists");

        }
        else {

            response.put("success", false);
            response.put("message", "Class not found");

        }

        return response;

    }


    /*
    Teacher login
    */
    @PostMapping("/auth/login")
    public Map<String,Object> login(@RequestBody Map<String,String> request) {

        String teacher_ID = request.get("teacher_ID");
        String password = request.get("password");

        Map<String,Object> response = new HashMap<>();

        if (teacher_ID.equals(validTeacher) && password.equals(validPassword)) {

            response.put("success", true);
            response.put("authToken", fakeToken);

        }
        else {

            response.put("success", false);
            response.put("message", "Invalid login");

        }

        return response;

    }


    /*
    Get classes
    */
    @GetMapping("/classes")
    public Map<String,Object> getClasses() {

        Map<String,Object> response = new HashMap<>();

        response.put("success", true);
        response.put("classList", classes.keySet());

        return response;

    }


    /*
    Get students in class
    */
    @GetMapping("/class/students")
    public Map<String,Object> getStudents(@RequestParam String classCode) {

        Map<String,Object> response = new HashMap<>();

        if (classes.containsKey(classCode)) {

            response.put("success", true);
            response.put("classList", classes.get(classCode));

        }
        else {

            response.put("success", false);

        }

        return response;

    }


    /*
    Compare students
    */
    @PostMapping("/compare")
    public Map<String,Object> compare(@RequestBody Map<String,String> request) {

        Map<String,Object> response = new HashMap<>();

        Random rand = new Random();

        double similarity = 50 + rand.nextDouble() * 50;

        response.put("success", true);
        response.put("similarityScore", Math.round(similarity * 10) / 10.0);

        return response;

    }


    /*
    Upload submission
    */
    @PostMapping("/submissions/upload")
    public Map<String,Object> upload(

        @RequestParam String classCode,
        @RequestParam String studentSub,
        @RequestParam MultipartFile submission

    ) {

        Map<String,Object> response = new HashMap<>();

        System.out.println("Received submission from: " + studentSub);
        System.out.println("File: " + submission.getOriginalFilename());

        response.put("success", true);
        response.put("submissionID", UUID.randomUUID().toString());

        return response;

    }

}
