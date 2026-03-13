package ca.group13.codecomparator.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/* Loads classes and students, then calls engine to perform comparison. */

@Service
public class ClassCompareService {

    private final JdbcTemplate jdbc;
    private final EngineCompareService engine;

    public ClassCompareService(JdbcTemplate jdbc, EngineCompareService engine) {
        this.jdbc = jdbc;
        this.engine = engine;
    }

    public List<String> listClasses() {
        return jdbc.query(
                "SELECT DISTINCT course_code FROM classes ORDER BY course_code",
                (rs, rowNum) -> rs.getString(1)
        );
    }

    public List<String> listStudents(String classCode) {
        if (classCode == null || classCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "classCode is required");
        }

        return jdbc.query("""
                SELECT DISTINCT s.encrypted_student
                FROM submissions s
                JOIN assignments a ON a.assignment_id = s.assignment_id
                JOIN classes c ON c.class_id = a.class_id
                WHERE c.course_code = ?
                ORDER BY s.encrypted_student
                """, (rs, rowNum) -> rs.getString(1), classCode.trim());
    }

    // Finds most recent submission for requested students in the provided class and sends ZIP keys to engine.
    public Map<String, Object> compareSubmissions(String classCode, String student1, String student2) {
        if (classCode == null || classCode.isBlank()) {
            return Map.of("success", false, "message", "classCode is required");
        }
        if (student1 == null || student1.isBlank() || student2 == null || student2.isBlank()) {
            return Map.of("success", false, "message", "student1 and student2 are required");
        }
        if (student1.equals(student2)) {
            return Map.of("success", false, "message", "Pick two different students");
        }

        SubmissionRef a = findSubmission(classCode.trim(), student1.trim());
        SubmissionRef b = findSubmission(classCode.trim(), student2.trim());

        if (a == null) {
            return Map.of("success", false, "message", "No submission found for student1 in this class");
        }
        if (b == null) {
            return Map.of("success", false, "message", "No submission found for student2 in this class");
        }

        // Engine team will compute similarity using the two ZIP blob keys
        Double similarityPercent = engine.comparePercent(a.submissionId, a.sourceZipKey, b.submissionId, b.sourceZipKey);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("similarityPercent", similarityPercent);
        out.put("message", similarityPercent == null ? "Similarity not computed yet (engine pending)" : "");
        out.put("student1SubmissionId", a.submissionId.toString());
        out.put("student2SubmissionId", b.submissionId.toString());
        out.put("student1ZipKey", a.sourceZipKey);
        out.put("student2ZipKey", b.sourceZipKey);
        return out;
    }

    private SubmissionRef findSubmission(String classCode, String studentNumber) {
        List<SubmissionRef> rows = jdbc.query("""
                SELECT s.submission_id, s.source_zip_key
                FROM submissions s
                JOIN assignments a ON a.assignment_id = s.assignment_id
                JOIN classes c ON c.class_id = a.class_id
                WHERE c.course_code = ?
                  AND s.encrypted_student = ?
                ORDER BY s.created_at DESC
                LIMIT 1
                """, (rs, rowNum) -> new SubmissionRef(UUID.fromString(rs.getString("submission_id")), rs.getString("source_zip_key")), classCode, studentNumber);

        return rows.isEmpty() ? null : rows.get(0);
    }

    private record SubmissionRef(UUID submissionId, String sourceZipKey) {
    }
}