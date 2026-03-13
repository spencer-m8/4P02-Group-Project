package ca.group13.codecomparator.controller;

import ca.group13.codecomparator.dto.CompareRequest;
import ca.group13.codecomparator.dto.CompareResponse;
import ca.group13.codecomparator.service.ClassCompareService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/* Helps run comparisons by getting the class list, students in the class, and
* sending requests to the service. */

@RestController
@RequestMapping("/api")
public class ClassCompareController {

    private final ClassCompareService service;

    public ClassCompareController(ClassCompareService service) {
        this.service = service;
    }

    // Matches frontend: GET /api/classes
    @GetMapping("/classes")
    public List<String> classes(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.listClasses();
    }

    @GetMapping("/class/students")
    public List<String> students(@RequestParam("classCode") String classCode, @RequestHeader(value = "Authorization", required = false) String auth) {
        return service.listStudents(classCode);
    }

    @PostMapping("/compare")
    public CompareResponse compare(@RequestBody CompareRequest body, @RequestHeader(value = "Authorization", required = false) String auth) {
        CompareResponse out = new CompareResponse();

        if (body == null) {
            out.setSuccess(false);
            out.setMessage("Request body is required");
            return out;
        }

        Map<String, Object> res = service.compareSubmissions(body.getClassCode(), body.getStudent1(), body.getStudent2());

        Object ok = res.get("success");
        boolean success = (ok instanceof Boolean b) ? b : Boolean.parseBoolean(String.valueOf(ok));
        out.setSuccess(success);

        if (!success) {
            Object msg = res.get("message");
            out.setMessage(msg == null ? "Compare failed" : msg.toString());
            return out;
        }

        Object score = res.get("similarityPercent");
        if (score instanceof Number n) {
            out.setSimilarityPercent(n.doubleValue());
        } else if (score != null) {
            try {
                out.setSimilarityPercent(Double.parseDouble(score.toString()));
            } catch (Exception ignored) {
                out.setSimilarityPercent(null);
            }
        }

        Object s1 = res.get("student1SubmissionId");
        Object s2 = res.get("student2SubmissionId");
        out.setStudent1SubmissionId(s1 == null ? null : s1.toString());
        out.setStudent2SubmissionId(s2 == null ? null : s2.toString());

        Object z1 = res.get("student1ZipKey");
        Object z2 = res.get("student2ZipKey");
        out.setStudent1ZipKey(z1 == null ? null : z1.toString());
        out.setStudent2ZipKey(z2 == null ? null : z2.toString());

        return out;
    }
}