package ca.group13.codecomparator.controller;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/seed")
public class SeedController {

    private final JdbcTemplate jdbc;
    private final BlobServiceClient blobServiceClient;

    private final String zipsContainer;
    private final String evidenceContainer;
    private final String normalizedContainer;

    public SeedController(JdbcTemplate jdbc, BlobServiceClient blobServiceClient, @Value("${app.storage.containers.zips:zips}") String zipsContainer, @Value("${app.storage.containers.evidence:evidence}") String evidenceContainer, @Value("${app.storage.containers.normalized:normalized}") String normalizedContainer) {
        this.jdbc = jdbc;
        this.blobServiceClient = blobServiceClient;
        this.zipsContainer = zipsContainer;
        this.evidenceContainer = evidenceContainer;
        this.normalizedContainer = normalizedContainer;
    }

    @GetMapping("/db/status")
    public Map<String, Object> dbStatus() {
        Map<String, Object> counts = Map.of("users", count("public.users"), "classes", count("public.classes"), "assignments", count("public.assignments"), "submissions", count("public.submissions"));

        return Map.of("success", true, "timestamp", Instant.now().toString(), "counts", counts);
    }

    @PostMapping("/db")
    public Map<String, Object> seedDbOnly() {
        try {
            return doSeedDbOnly();
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, safeDbMessage(e), e);
        }
    }

    @Transactional
    Map<String, Object> doSeedDbOnly() {
        UUID instructorId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (user_id, user_email_enc, password_hash, role)
                VALUES (?, ?, ?, ?)
                """, instructorId, "instructor@example.com", "pass", "INSTRUCTOR");

        UUID classId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO classes (class_id, course_code, term, section, instructor_user_id)
                VALUES (?, ?, ?, ?, ?)
                """, classId, "COSC4P02", "W2026", "001", instructorId);

        // One assignment with a unique 10 digit key
        AssignmentSeed assignment = insertAssignment(classId, "DB Seed Assignment", "JAVA");

        return Map.of("success", true, "timestamp", Instant.now().toString(), "instructorId", instructorId.toString(), "classId", classId.toString(), "assignment", assignment.toMap());
    }

    // Upload a small blob to confirm Azure connection
    @PostMapping("/blob")
    public Map<String, Object> seedBlobOnly() {
        try {
            BlobContainerClient container = ensureContainer(zipsContainer);

            String key = "seed/hello-" + UUID.randomUUID() + ".txt";
            byte[] bytes = ("hello @ " + Instant.now()).getBytes(StandardCharsets.UTF_8);

            container.getBlobClient(key).upload(new ByteArrayInputStream(bytes), bytes.length, true);

            return Map.of("success", true, "timestamp", Instant.now().toString(), "container", zipsContainer, "blobKey", key, "bytes", bytes.length);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    /*
     * Clears DB rows + blob containers and creates:
     * - 1 instructor
     * - 2 classes
     * - 2 assignments per class
     * - 1 submission per assignment
     * and uploads small ZIP blobs.
     */
    @PostMapping("/reset-and-seed")
    public Map<String, Object> resetAndSeed(@RequestBody Map<String, Object> body) {
        String confirm = Objects.toString(body.getOrDefault("confirm", ""), "").trim();
        if (!"RESET".equals(confirm)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing/invalid confirm. Send JSON like: {\"confirm\":\"RESET\"}");
        }

        try {
            return doResetAndSeed();
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, safeDbMessage(e), e);
        }
    }

    @Transactional
    Map<String, Object> doResetAndSeed() {
        // Clear blob containers
        List<String> cleared = new ArrayList<>();
        if (tryClearContainer(zipsContainer)) cleared.add(zipsContainer);
        if (tryClearContainer(evidenceContainer)) cleared.add(evidenceContainer);
        if (tryClearContainer(normalizedContainer)) cleared.add(normalizedContainer);

        // Clear DB rows
        jdbc.execute("""
                TRUNCATE TABLE
                  public.repository_items,
                  public.submissions,
                  public.repositories,
                  public.assignments,
                  public.classes,
                  public.users
                CASCADE
                """);

        UUID instructorId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (user_id, user_email_enc, password_hash, role)
                VALUES (?, ?, ?, ?)
                """, instructorId, "instructor@example.com", "pass", "INSTRUCTOR");

        UUID class4P02 = UUID.randomUUID();
        UUID class4P91 = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO classes (class_id, course_code, term, section, instructor_user_id)
                VALUES (?, ?, ?, ?, ?)
                """, class4P02, "COSC4P02", "W2026", "001", instructorId);

        jdbc.update("""
                INSERT INTO classes (class_id, course_code, term, section, instructor_user_id)
                VALUES (?, ?, ?, ?, ?)
                """, class4P91, "COSC4P91", "W2026", "001", instructorId);

        AssignmentSeed a1 = insertAssignment(class4P02, "A1 - Intro", "JAVA");
        AssignmentSeed a2 = insertAssignment(class4P02, "A2 - Part B", "JAVA");

        AssignmentSeed b1 = insertAssignment(class4P91, "A1 - C Basics", "C");
        AssignmentSeed b2 = insertAssignment(class4P91, "A2 - CPP Basics", "CPP");

        List<Map<String, Object>> submissions = new ArrayList<>();
        submissions.add(submissionFromZip(zipsContainer, a1.assignmentId, a1.language, "10000001"));
        submissions.add(submissionFromZip(zipsContainer, a2.assignmentId, a2.language, "10000002"));
        submissions.add(submissionFromZip(zipsContainer, b1.assignmentId, b1.language, "10000003"));
        submissions.add(submissionFromZip(zipsContainer, b2.assignmentId, b2.language, "10000004"));

        return Map.of("success", true, "timestamp", Instant.now().toString(), "clearedContainers", cleared, "instructor", Map.of("userId", instructorId.toString(), "email", "instructor@example.com", "password", "pass"), "classes", List.of(Map.of("courseCode", "COSC4P02", "classId", class4P02.toString()), Map.of("courseCode", "COSC4P91", "classId", class4P91.toString())), "assignments", List.of(a1.toMap(), a2.toMap(), b1.toMap(), b2.toMap()), "submissions", submissions);
    }

    private int count(String table) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return (n == null) ? 0 : n;
    }

    private String safeDbMessage(DataAccessException e) {
        Throwable root = e.getMostSpecificCause();
        if (root != null && root.getMessage() != null) return root.getMessage();
        return e.getMessage();
    }

    private BlobContainerClient ensureContainer(String containerName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        if (!container.exists()) container.create();
        return container;
    }

    private boolean tryClearContainer(String containerName) {
        try {
            BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
            if (!container.exists()) return false;

            for (BlobItem item : container.listBlobs()) {
                container.getBlobClient(item.getName()).delete();
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private AssignmentSeed insertAssignment(UUID classId, String name, String language) {
        UUID assignmentId = UUID.randomUUID();
        String key = randAssignKey();

        jdbc.update("""
                INSERT INTO assignments (assignment_id, class_id, assignment_name, assignment_key, language)
                VALUES (?, ?, ?, ?, ?)
                """, assignmentId, classId, name, key, language);

        return new AssignmentSeed(assignmentId, name, key, language);
    }

    private String randAssignKey() {
        SecureRandom r = new SecureRandom();
        while (true) {
            String key = tenD(r);

            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM assignments WHERE assignment_key = ?", Integer.class, key);

            if (count != null && count == 0) return key;
        }
    }

    private static String tenD(SecureRandom r) {
        int first = 1 + r.nextInt(9); // first digit will not be 0
        StringBuilder sb = new StringBuilder(10);
        sb.append(first);
        for (int i = 0; i < 9; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    private Map<String, Object> submissionFromZip(String containerName, UUID assignmentId, String language, String studentNumber) {
        UUID submissionId = UUID.randomUUID();

        // Blob key inside the container
        String blobKey = "submissions/" + submissionId + ".zip";

        byte[] zipBytes = makeZip(language);

        BlobContainerClient container = ensureContainer(containerName);
        container.getBlobClient(blobKey).upload(new ByteArrayInputStream(zipBytes), zipBytes.length, true);

        String sha = sha256Hex(zipBytes);

        jdbc.update("""
                INSERT INTO submissions (
                    submission_id, assignment_id, encrypted_student,
                    source_zip_key, zip_sha256, language
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """, submissionId, assignmentId, studentNumber, blobKey, sha, language);

        return Map.of("submissionId", submissionId.toString(), "assignmentId", assignmentId.toString(), "studentNumber", studentNumber, "language", language, "sourceZipKey", blobKey);
    }

    private static byte[] makeZip(String languageRaw) {
        String language = (languageRaw == null) ? "" : languageRaw.trim().toUpperCase(Locale.ROOT);

        String fileName;
        String content;

        switch (language) {
            case "JAVA" -> {
                fileName = "Main.java";
                content = """
                        public class Main {
                            public static void main(String[] args) {
                                System.out.println("Hello from seeded Java!");
                            }
                        }
                        """;
            }
            case "C" -> {
                fileName = "main.c";
                content = """
                        #include <stdio.h>
                        int main() {
                            printf("Hello from seeded C!\\n");
                            return 0;
                        }
                        """;
            }
            case "CPP" -> {
                fileName = "main.cpp";
                content = """
                        #include <iostream>
                        int main() {
                            std::cout << "Hello from seeded C++!" << std::endl;
                            return 0;
                        }
                        """;
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown language: " + language);
        }

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try (ZipOutputStream zout = new ZipOutputStream(bout)) {
                zout.putNextEntry(new ZipEntry(fileName));
                zout.write(content.getBytes(StandardCharsets.UTF_8));
                zout.closeEntry();
            }
            return bout.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build test zip", e);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);

            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    private static class AssignmentSeed {
        final UUID assignmentId;
        final String name;
        final String key;
        final String language;

        AssignmentSeed(UUID assignmentId, String name, String key, String language) {
            this.assignmentId = assignmentId;
            this.name = name;
            this.key = key;
            this.language = language;
        }

        Map<String, Object> toMap() {
            return Map.of("assignmentId", assignmentId.toString(), "assignmentName", name, "assignmentKey", key, "language", language);
        }
    }
}