package edu.univ.erp.domain.grades;

public class AssignmentComponent extends GradeComponent {
  public AssignmentComponent(String name, double score, double maxScore, double weightage) {
    super(GradeType.ASSIGNMENT, name, score, maxScore, weightage);
  }
}
