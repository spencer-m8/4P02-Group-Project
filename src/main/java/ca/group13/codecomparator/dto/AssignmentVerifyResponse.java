package ca.group13.codecomparator.dto;

public class AssignmentVerifyResponse {
    private boolean success;
    private String message;
    private String assignmentKey;
    private AssignmentInfo info;

    public AssignmentVerifyResponse() {
    }

    public AssignmentVerifyResponse(boolean success, String message, String assignmentKey, AssignmentInfo info) {
        this.success = success;
        this.message = message;
        this.assignmentKey = assignmentKey;
        this.info = info;
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

    public String getAssignmentKey() {
        return assignmentKey;
    }

    public void setAssignmentKey(String assignmentKey) {
        this.assignmentKey = assignmentKey;
    }

    public AssignmentInfo getInfo() {
        return info;
    }

    public void setInfo(AssignmentInfo info) {
        this.info = info;
    }
}