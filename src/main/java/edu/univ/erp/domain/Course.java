package edu.univ.erp.domain;

public class Course {

    private final int courseId;
    private final String code;
    private final String title;
    private final String description;
    private final int credits;

    public Course(int courseId, String code, String title, String description, int credits) {
        this.courseId = courseId;
        this.code = code;
        this.title = title;
        this.description = description;
        this.credits = credits;
    }

    public int getCourseId() {
        return courseId;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getCredits() {
        return credits;
    }

    @Override
    public String toString() {
        return code + " — " + title;
    }
}
