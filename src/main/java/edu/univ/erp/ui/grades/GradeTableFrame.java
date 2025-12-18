package edu.univ.erp.ui.grades;

import edu.univ.erp.domain.grades.GradeComponent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GradeTableFrame extends JFrame {

  private static final Logger log = LoggerFactory.getLogger(GradeTableFrame.class);

  public GradeTableFrame(List<GradeComponent> comps, String finalGrade) {
    super("My Grades");
    log.debug("Initializing GradeTableFrame with {} grade components", comps.size());

    setSize(700, 400);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    String[] cols = { "Type", "Name", "Score", "Max", "Weightage", "Contribution" };

    DefaultTableModel model = new DefaultTableModel(cols, 0) {
      @Override
      public boolean isCellEditable(int r, int c) {
        return false;
      }
    };

    for (GradeComponent g : comps) {
      log.debug("Adding grade component to table: type={}, name={}", g.getType(), g.getName());

      model.addRow(new Object[] {
          g.getType().name(),
          g.getName(),
          String.format("%.2f", g.getScore()),
          String.format("%.2f", g.getMaxScore()),
          String.format("%.2f%%", g.getWeightage()),
          String.format("%.2f", g.calculateContribution())
      });
    }

    JTable table = new JTable(model);
    table.setRowHeight(28);

    JScrollPane scroll = new JScrollPane(table);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JLabel finalLbl = new JLabel("Final Grade: " + (finalGrade == null ? "-" : finalGrade));
    finalLbl.setFont(finalLbl.getFont().deriveFont(Font.BOLD, 14f));

    bottom.add(finalLbl);

    setLayout(new BorderLayout());
    add(scroll, BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);

    log.info("GradeTableFrame initialized and ready (finalGrade={})", finalGrade);
  }
}
