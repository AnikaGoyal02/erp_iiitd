package edu.univ.erp.domain;

public class Section {
  private final int sectionId;
  private final int courseId;
  private final Integer instructorId;
  private final String dayTime;
  private final String room;
  private final int capacity;
  private final String semester;
  private final int year;
  
  private String courseCode;
  private String courseTitle;
  private String instructorEmail;
  private int credits;

  public Section(int sectionId, int courseId, Integer instructorId, String dayTime, String room, int capacity, String semester, int year) {
    this.sectionId = sectionId;
    this.courseId = courseId;
    this.instructorId = instructorId;
    this.dayTime = dayTime;
    this.room = room;
    this.capacity = capacity;
    this.semester = semester;
    this.year = year;
  }

  public int getSectionId() {
    return sectionId;
  }

  public int getCourseId() {
    return courseId;
  }

  public Integer getInstructorId() {
    return instructorId;
  }

  public String getDayTime() {
    return dayTime;
  }

  public String getRoom() {
    return room;
  }

  public int getCapacity() {
    return capacity;
  }

  public String getSemester() {
    return semester;
  }

  public int getYear() {
    return year;
  }


  public String getCourseCode() {
    return courseCode; 
  }
  public void setCourseCode(String courseCode) { this.courseCode = courseCode; 
  }

  public String getCourseTitle() { 
    return courseTitle; 
  }
  public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

  public String getInstructorEmail() { return instructorEmail; }
  public void setInstructorEmail(String instructorEmail) { this.instructorEmail = instructorEmail; }

  public int getCredits() { return credits; }
  public void setCredits(int credits) { this.credits = credits; }

  @Override
  public String toString() {
    return "Section " + sectionId + " - " + dayTime + " - " + semester + " " + year;
  }
}
