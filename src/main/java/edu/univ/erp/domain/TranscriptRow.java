package edu.univ.erp.domain;

public class TranscriptRow {

  private final String courseCode;
  private final String courseTitle;
  private final int credits;
  private final String semester;
  private final int year;
  private final String finalGrade;

  public TranscriptRow(String courseCode, String courseTitle, int credits, String semester, int year,
      String finalGrade) {
    this.courseCode = courseCode;
    this.courseTitle = courseTitle;
    this.credits = credits;
    this.semester = semester;
    this.year = year;
    this.finalGrade = finalGrade;
  }

  public String getCourseCode() {
    return courseCode;
  }

  public String getCourseTitle() {
    return courseTitle;
  }

  public int getCredits() {
    return credits;
  }

  public String getSemester() {
    return semester;
  }

  public int getYear() {
    return year;
  }

  public String getFinalGrade() {
    return finalGrade;
  }
}
