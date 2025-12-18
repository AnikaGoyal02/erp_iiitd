package edu.univ.erp.domain.grades;

import java.util.*;

public class Gradebook {
  private final int enrollmentId;
  private final List<GradeComponent> components = new ArrayList<>();

  public Gradebook(int enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public int getEnrollmentId() {
    return enrollmentId;
  }

  public void addComponent(GradeComponent c) {
    if (c == null)
      throw new IllegalArgumentException("component cannot be null");
    components.add(c);
  }

  public void removeComponent(GradeComponent c) {
    components.remove(c);
  }

  public List<GradeComponent> getComponents() {
    return Collections.unmodifiableList(components);
  }

  public double computeFinalNumeric() {
    return components.stream()
        .mapToDouble(GradeComponent::calculateContribution)
        .sum();
  }

  public String computeFinalLetter() {
    double x = computeFinalNumeric();
      if (x>=95) return "A+" ; 
      if (x>=90) return "A" ; 
      if (x >= 85) return "A-";
      if (x >= 75) return "B";
      if (x >= 65) return "B-";
      if (x >= 55) return "C";
      if (x >= 45) return "D";
      return "F";
    }




  @Override
  public String toString() {
    return String.format("Gradebook{enrollment=%d, final=%.2f, letter=%s}", enrollmentId, computeFinalNumeric(),
        computeFinalLetter());
  }
}
