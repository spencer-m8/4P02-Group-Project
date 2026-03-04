package ca.group13.codecomparator.controller;

import ca.group13.codecomparator.dto.AssignmentInfo;
import ca.group13.codecomparator.dto.AssignmentVerifyRequest;
import ca.group13.codecomparator.dto.AssignmentVerifyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/assignment")
public class ApiAssignmentController {

    private final JdbcTemplate jdbc;

    public ApiAssignmentController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/verify")
    public AssignmentVerifyResponse verify(@RequestBody AssignmentVerifyRequest body) {
        String assignmentKey = (body == null || body.getAssignmentKey() == null) ? "" : body.getAssignmentKey().trim();

        if (!assignmentKey.matches("^[0-9]{10}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assignmentKey must be exactly 10 digits");
        }

        AssignmentInfo info = jdbc.query("""
                SELECT a.assignment_name, a.language, c.course_code
                FROM assignments a
                JOIN classes c ON c.class_id = a.class_id
                WHERE a.assignment_key = ?
                """, rs -> rs.next() ? new AssignmentInfo(rs.getString("assignment_name"), rs.getString("language"), rs.getString("course_code")) : null, assignmentKey);

        if (info == null) {
            return new AssignmentVerifyResponse(false, "Assignment key not found", null, null);
        }

        return new AssignmentVerifyResponse(true, null, assignmentKey, info);
    }
}