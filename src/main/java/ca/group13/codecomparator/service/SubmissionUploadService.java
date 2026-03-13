package ca.group13.codecomparator.service;

import ca.group13.codecomparator.dto.SubmissionUploadResponse;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/* Main service for storing submissions. Checks ZIP, uploads to blob, and saves details in DB. */

@Service
public class SubmissionUploadService {

    private static final long MB = 1024L * 1024L;
    private static final long MAX_ZIP_BYTES = 50L * MB; // 50 MB cap

    private final JdbcTemplate jdbc;
    private final BlobServiceClient blobServiceClient;
    private final String containerName;

    public SubmissionUploadService(JdbcTemplate jdbc, BlobServiceClient blobServiceClient, @Value("${app.storage.containers.test}") String containerName) {
        this.jdbc = jdbc;
        this.blobServiceClient = blobServiceClient;
        this.containerName = containerName;
    }

    public SubmissionUploadResponse submissionFromZip(byte[] zipBytes, UUID assignmentId, String studentNumber, String language, Boolean late) {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }
        if (assignmentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assignmentId is required");
        }
        if (studentNumber == null || studentNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentNumber is required");
        }

        String lang = normalizeLang(language);

        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM assignments WHERE assignment_id = ?", Integer.class, assignmentId);
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assignmentId not found: " + assignmentId);
        }

        if (zipBytes.length > MAX_ZIP_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ZIP too large (max 50 MB)");
        }

        validateZip(zipBytes, lang);

        UUID submissionId = UUID.randomUUID();
        String blobKey = "submissions/" + submissionId + ".zip";

        // Build the proper path for blob storage
        BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(blobKey);

        // Upload the ZIP bytes to the blob at the generated path
        try {
            try (ByteArrayInputStream in = new ByteArrayInputStream(zipBytes)) {
                blobClient.upload(in, zipBytes.length, true);
            }

            insertSubmissionRow(submissionId, assignmentId, studentNumber, blobKey, lang, late);

            SubmissionUploadResponse out = new SubmissionUploadResponse();
            out.setSuccess(true);
            out.setSubmissionId(submissionId);
            out.setAssignmentId(assignmentId);
            out.setLanguage(lang);
            out.setSourceZipKey(blobKey);
            return out;

        } catch (DataAccessException dbEx) {
            try {
                blobClient.deleteIfExists();
            } catch (Exception ignored) {
            }

            Throwable root = dbEx.getMostSpecificCause();
            String msg = (root != null && root.getMessage() != null) ? root.getMessage() : dbEx.getMessage();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, msg, dbEx);

        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed: " + ex.getMessage(), ex);
        }
    }

    // Insert submission details to DB
    @Transactional
    void insertSubmissionRow(UUID submissionId, UUID assignmentId, String studentNumber, String blobKey, String lang, Boolean late) {
        if (late == null) {
            jdbc.update("""
                    INSERT INTO submissions
                      (submission_id, assignment_id, encrypted_student, source_zip_key, language)
                    VALUES
                      (?, ?, ?, ?, ?)
                    """, submissionId, assignmentId, studentNumber, blobKey, lang);
        } else {
            jdbc.update("""
                    INSERT INTO submissions
                      (submission_id, assignment_id, late, encrypted_student, source_zip_key, language)
                    VALUES
                      (?, ?, ?, ?, ?, ?)
                    """, submissionId, assignmentId, late, studentNumber, blobKey, lang);
        }
    }

    private static String normalizeLang(String language) {
        if (language == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "language is required");
        }

        String up = language.trim().toUpperCase(Locale.ROOT);
        if (up.equals("C++")) up = "CPP";

        if (!up.equals("JAVA") && !up.equals("C") && !up.equals("CPP")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "language must be JAVA, C, or CPP");
        }
        return up;
    }

    // Confirms the ZIP contains valid project before upload
    private static void validateZip(byte[] zipBytes, String lang) {
        boolean foundFile = false;
        boolean foundExpected = false;

        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.isDirectory()) continue;

                foundFile = true;
                String name = e.getName().toLowerCase(Locale.ROOT);

                if (lang.equals("JAVA") && name.endsWith(".java")) foundExpected = true;
                if (lang.equals("C") && name.endsWith(".c")) foundExpected = true;

                if (lang.equals("CPP")) {
                    if (name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".cxx") || name.endsWith(".hpp") || name.endsWith(".hh") || name.endsWith(".hxx") || name.endsWith(".h")) {
                        foundExpected = true;
                    }
                }
            }
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is not a valid ZIP");
        }

        if (!foundFile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ZIP is empty");
        }
        if (!foundExpected) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ZIP does not contain expected source files for " + lang);
        }
    }
}