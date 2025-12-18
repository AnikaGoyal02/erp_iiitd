package edu.univ.erp.domain.grades;

public class ProjectComponent extends GradeComponent {
  public ProjectComponent(String name, double score, double maxScore, double weightage) {
    super(GradeType.PROJECT, name, score, maxScore, weightage);
  }
}
