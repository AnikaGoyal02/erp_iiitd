package edu.univ.erp.domain;

public abstract class User {
  protected final int userId;
  protected final String username;
  protected final Role role;

  public User(int userId, String username, Role role) {
    this.userId = userId;
    this.username = username;
    this.role = role;
  }

  public int getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public Role getRole() {
    return role;
  }

  @Override
  public String toString() {
    return String.format("%s{id=%d, username=%s}", role, userId, username);
  }
}
