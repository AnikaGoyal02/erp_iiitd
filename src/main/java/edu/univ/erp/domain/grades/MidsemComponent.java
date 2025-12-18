package edu.univ.erp.domain.grades;

public class MidsemComponent extends GradeComponent {
  public MidsemComponent(String name, double score, double maxScore, double weightage) {
    super(GradeType.MIDSEM, name, score, maxScore, weightage);
  }
}
