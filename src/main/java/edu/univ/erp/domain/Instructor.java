package edu.univ.erp.domain;

public class Instructor extends User {
  private final String department;
  private final String email;


  public Instructor(int userId, String username, String department, String email) {
    super(userId, username, Role.INSTRUCTOR);
    this.department = department;
    this.email = email;
  }

  public String getDepartment() {
    return department;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public String toString() {
    return username + " (" + department + ")";
  }
}
