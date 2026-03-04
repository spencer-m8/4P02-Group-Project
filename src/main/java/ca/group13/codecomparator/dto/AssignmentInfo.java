package ca.group13.codecomparator.dto;

public class AssignmentInfo {
    private String assignmentName;
    private String language;
    private String courseCode;

    public AssignmentInfo() {
    }

    public AssignmentInfo(String assignmentName, String language, String courseCode) {
        this.assignmentName = assignmentName;
        this.language = language;
        this.courseCode = courseCode;
    }

    public String getAssignmentName() {
        return assignmentName;
    }

    public void setAssignmentName(String assignmentName) {
        this.assignmentName = assignmentName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }
}