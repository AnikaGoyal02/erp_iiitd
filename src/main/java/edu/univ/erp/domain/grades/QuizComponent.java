package edu.univ.erp.domain.grades;

public class QuizComponent extends GradeComponent {
  public QuizComponent(String name, double score, double maxScore, double weightage) {
    super(GradeType.QUIZ, name, score, maxScore, weightage);
  }
}
