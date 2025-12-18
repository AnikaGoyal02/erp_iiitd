package edu.univ.erp.domain;

public class Admin extends User {
  public Admin(int userId, String username) {
    super(userId, username, Role.ADMIN);
  }
}
