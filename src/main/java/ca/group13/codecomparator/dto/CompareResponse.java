package ca.group13.codecomparator.dto;

public class CompareResponse {
    private boolean success;
    private String message;

    private Double similarityPercent;

    private String student1SubmissionId;
    private String student2SubmissionId;
    private String student1ZipKey;
    private String student2ZipKey;

    public CompareResponse() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Double getSimilarityPercent() {
        return similarityPercent;
    }

    public void setSimilarityPercent(Double similarityPercent) {
        this.similarityPercent = similarityPercent;
    }

    public String getStudent1SubmissionId() {
        return student1SubmissionId;
    }

    public void setStudent1SubmissionId(String student1SubmissionId) {
        this.student1SubmissionId = student1SubmissionId;
    }

    public String getStudent2SubmissionId() {
        return student2SubmissionId;
    }

    public void setStudent2SubmissionId(String student2SubmissionId) {
        this.student2SubmissionId = student2SubmissionId;
    }

    public String getStudent1ZipKey() {
        return student1ZipKey;
    }

    public void setStudent1ZipKey(String student1ZipKey) {
        this.student1ZipKey = student1ZipKey;
    }

    public String getStudent2ZipKey() {
        return student2ZipKey;
    }

    public void setStudent2ZipKey(String student2ZipKey) {
        this.student2ZipKey = student2ZipKey;
    }
}