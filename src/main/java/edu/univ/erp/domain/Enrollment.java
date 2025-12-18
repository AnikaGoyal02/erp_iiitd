package edu.univ.erp.domain;

public class Enrollment {
  public enum Status {
    ENROLLED, DROPPED, COMPLETED
  }

  private final int enrollmentId;
  private final int studentId;
  private final int sectionId;
  private final Status status;
  private final String registeredOn;
  private final String droppedOn;
  private final String finalGrade;

  private String studentRollNo;
  private String studentEmail;
  private String studentName;


  private Course course;

  public Enrollment(int enrollmentId, int studentId, int sectionId, Status status, String registeredOn,
      String droppedOn, String finalGrade) {
    this.enrollmentId = enrollmentId;
    this.studentId = studentId;
    this.sectionId = sectionId;
    this.status = status;
    this.registeredOn = registeredOn;
    this.droppedOn = droppedOn;
    this.finalGrade = finalGrade;
  }

  public int getEnrollmentId() {
    return enrollmentId;
  }

  public int getStudentId() {
    return studentId;
  }

  public int getSectionId() {
    return sectionId;
  }

  public Status getStatus() {
    return status;
  }

  public String getRegisteredOn() {
    return registeredOn;
  }

  public String getDroppedOn() {
    return droppedOn;
  }

  public String getFinalGrade() {
    return finalGrade;
  }

  public Course getCourse() {
    return course;
  }

  public void setCourse(Course course) {
    this.course = course;
  }


  public void setStudentRollNo(String rollNo) { this.studentRollNo = rollNo; 
  }

  public String getStudentRollNo() { return studentRollNo; 
  }

  public void setStudentEmail(String email) { this.studentEmail = email; }
  public String getStudentEmail() { return studentEmail; 
  }


  public void setStudentName(String name) { this.studentName = name; }
  public String getStudentName() { return studentName; }

  @Override
  public String toString() {
    return String.format("Enrollment{id=%d, student=%d, section=%d, status=%s}", enrollmentId, studentId, sectionId,
        status);
  }
}
