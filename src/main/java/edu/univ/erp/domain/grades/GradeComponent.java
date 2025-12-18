package edu.univ.erp.domain.grades;

public abstract class GradeComponent {
  protected final GradeType type;
  protected final String name;
  protected final double score;
  protected final double maxScore;
  protected final double weightage;

  public GradeComponent(GradeType type, String name, double score, double maxScore, double weightage) {
    if (maxScore <= 0)
      throw new IllegalArgumentException("maxScore must be > 0");
    this.type = type;
    this.name = name;
    this.score = score;
    this.maxScore = maxScore;
    this.weightage = weightage;
  }

  public GradeType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public double getScore() {
    return score;
  }

  public double getMaxScore() {
    return maxScore;
  }

  public double getWeightage() {
    return weightage;
  }

  public double calculateContribution() {
    return (score / maxScore) * weightage;
  }


  public double weightedScore() {
    double pct = (score / maxScore) * 100.0;
    return pct * (weightage / 100.0);
  }


  @Override
  public String toString() {
    return String.format("%s{name=%s, score=%.2f/%.2f, weight=%.2f}", type, name, score, maxScore, weightage);
  }
}
