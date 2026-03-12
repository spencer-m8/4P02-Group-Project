package ca.group13.codecomparator.controller;

import ca.group13.codecomparator.dto.SubmissionUploadRequest;
import ca.group13.codecomparator.dto.SubmissionUploadResponse;
import ca.group13.codecomparator.service.SubmissionUploadService;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/api/submissions")
public class ApiSubmissionController {

    private final JdbcTemplate jdbc;
    private final SubmissionUploadService uploadService;
    private final BlobServiceClient blobServiceClient;
    private final String zipContainer;

    public ApiSubmissionController(JdbcTemplate jdbc, SubmissionUploadService uploadService, BlobServiceClient blobServiceClient, @Value("${app.storage.containers.zips:zips}") String zipContainer) {
        this.jdbc = jdbc;
        this.uploadService = uploadService;
        this.blobServiceClient = blobServiceClient;
        this.zipContainer = zipContainer;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SubmissionUploadResponse upload(@RequestPart("file") MultipartFile file, @ModelAttribute SubmissionUploadRequest form) {
        String key = (form == null || form.getAssignmentKey() == null) ? "" : form.getAssignmentKey().trim();
        String student = (form == null || form.getStudentNumber() == null) ? "" : form.getStudentNumber().trim();

        if (!key.matches("^[0-9]{10}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assignmentKey must be exactly 10 digits");
        }
        if (student.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentNumber is required");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }

        var assignmentRow = jdbc.query("SELECT assignment_id, language FROM assignments WHERE assignment_key = ?", rs -> rs.next() ? Map.<String, Object>of("assignmentId", UUID.fromString(rs.getString("assignment_id")), "expectedLanguage", rs.getString("language")) : null, key);

        if (assignmentRow == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assignment key");
        }

        UUID assignmentId = (UUID) assignmentRow.get("assignmentId");
        String expectedLang = (String) assignmentRow.get("expectedLanguage"); // may be null

        byte[] zipBytes;
        try {
            zipBytes = file.getBytes();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }

        String inferred = findLang(zipBytes);

        if (expectedLang != null && !expectedLang.isBlank()) {
            String expectedUp = expectedLang.trim().toUpperCase(Locale.ROOT);
            if (!expectedUp.equals(inferred)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment expects " + expectedUp + " but your ZIP looks like " + inferred);
            }
        }

        Map<String, Object> core = uploadService.submissionFromZip(zipBytes, assignmentId, student, inferred, null);

        SubmissionUploadResponse out = new SubmissionUploadResponse();
        out.setSuccess(true);
        out.setAssignmentKey(key);

        out.setSubmissionId((UUID) core.get("submissionId"));
        out.setAssignmentId((UUID) core.get("assignmentId"));
        out.setLanguage(core.get("language") == null ? null : core.get("language").toString());
        out.setSourceZipKey(core.get("sourceZipKey") == null ? null : core.get("sourceZipKey").toString());
        out.setZipSha256(core.get("zipSha256") == null ? null : core.get("zipSha256").toString());

        return out;
    }

    @GetMapping("/source")
    public Map<String, Object> source(@RequestParam("zipKey") String zipKey) {
        if (zipKey == null || zipKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zipKey is required");
        }

        var blob = blobServiceClient.getBlobContainerClient(zipContainer).getBlobClient(zipKey.trim());

        if (!blob.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found");
        }

        byte[] zipBytes;
        try (var stream = blob.openInputStream()) {
            zipBytes = stream.readAllBytes();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read submission");
        }

        Set<String> sourceExts = Set.of(".java", ".c", ".cpp", ".cc", ".cxx", ".hpp", ".hh", ".hxx", ".h");
        StringBuilder sb = new StringBuilder();

        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                String lower = name.toLowerCase(Locale.ROOT);
                boolean isSource = sourceExts.stream().anyMatch(lower::endsWith);
                if (isSource) {
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append("// --- ").append(name).append(" ---\n");
                    sb.append(new String(zin.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read ZIP contents");
        }

        return Map.of("text", sb.toString());
    }

    private static String findLang(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }

        boolean hasJava = false;
        boolean hasC = false;
        boolean hasCpp = false;

        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String name = e.getName().toLowerCase(Locale.ROOT);

                if (name.endsWith(".java")) hasJava = true;
                if (name.endsWith(".c")) hasC = true;

                if (name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".cxx") || name.endsWith(".hpp") || name.endsWith(".hh") || name.endsWith(".hxx")) {
                    hasCpp = true;
                }
            }
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is not a valid ZIP");
        }

        int signals = (hasJava ? 1 : 0) + (hasC ? 1 : 0) + (hasCpp ? 1 : 0);
        if (signals == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ZIP does not contain Java/C/C++ source files");
        }

        if ((hasJava && (hasC || hasCpp)) || (hasCpp && hasC)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ZIP appears to contain multiple languages. Please upload only one language (JAVA, C, or CPP).");
        }

        if (hasJava) return "JAVA";
        if (hasCpp) return "CPP";
        return "C";
    }
}