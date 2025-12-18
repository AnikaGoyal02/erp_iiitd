package edu.univ.erp.domain;

public class Student extends User {
  private final String rollNo;
  private final String program;
  private final int year;
  private final String email;

  public Student(int userId, String username, String rollNo, String program, int year, String email) {
    super(userId, username, Role.STUDENT);
    this.rollNo = rollNo;
    this.program = program;
    this.year = year;
    this.email = email;
  }

  public String getRollNo() {
    return rollNo;
  }

  public String getProgram() {
    return program;
  }

  public int getYear() {
    return year;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public String toString() {
    return String.format("Student{id=%d, username=%s, roll=%s, program=%s, year=%d}",
        userId, username, rollNo, program, year);
  }
}
