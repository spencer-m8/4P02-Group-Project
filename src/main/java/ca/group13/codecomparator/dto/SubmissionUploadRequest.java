package ca.group13.codecomparator.dto;

public class SubmissionUploadRequest {

    private String assignmentKey;
    private String studentNumber;

    public SubmissionUploadRequest() {
    }

    public String getAssignmentKey() {
        return assignmentKey;
    }

    public void setAssignmentKey(String assignmentKey) {
        this.assignmentKey = assignmentKey;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }
}