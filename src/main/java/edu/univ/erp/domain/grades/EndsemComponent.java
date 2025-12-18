package edu.univ.erp.domain.grades;

public class EndsemComponent extends GradeComponent {
  public EndsemComponent(String name, double score, double maxScore, double weightage) {
    super(GradeType.ENDSEM, name, score, maxScore, weightage);
  }
}
