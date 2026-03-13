package ca.group13.codecomparator.dto;

import java.util.UUID;

public class SubmissionUploadResponse {
    private boolean success;
    private String assignmentKey;

    private UUID submissionId;
    private UUID assignmentId;
    private String language;
    private String sourceZipKey;

    public SubmissionUploadResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getAssignmentKey() {
        return assignmentKey;
    }

    public void setAssignmentKey(String assignmentKey) {
        this.assignmentKey = assignmentKey;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(UUID submissionId) {
        this.submissionId = submissionId;
    }

    public UUID getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(UUID assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSourceZipKey() {
        return sourceZipKey;
    }

    public void setSourceZipKey(String sourceZipKey) {
        this.sourceZipKey = sourceZipKey;
    }
}