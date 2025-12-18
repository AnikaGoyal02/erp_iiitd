package edu.univ.erp.ui.admin;

import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.ui.UIError;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssignInstructorDialog extends JDialog {

  private static final Logger log = LoggerFactory.getLogger(AssignInstructorDialog.class);

  private final AdminService svc = new AdminService();

  public AssignInstructorDialog(Window owner) {
    super(owner, "Assign Instructor", ModalityType.APPLICATION_MODAL);
    log.debug("Opening AssignInstructorDialog");
    init();
  }

  private void init() {
    log.debug("Loading sections and instructors for assignment dialog");
    List<Section> sections = svc.listSections();
    List<Instructor> instructors = svc.listInstructors();

    setLayout(new BorderLayout(10, 10));
    JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

    JComboBox<Section> sectionBox = new JComboBox<>(sections.toArray(new Section[0]));
    JComboBox<Instructor> instrBox = new JComboBox<>(instructors.toArray(new Instructor[0]));

    form.add(new JLabel("Section:"));
    form.add(sectionBox);
    form.add(new JLabel("Instructor:"));
    form.add(instrBox);

    add(form, BorderLayout.CENTER);

    JPanel btns = new JPanel();
    JButton ok = new JButton("Assign");
    JButton cancel = new JButton("Cancel");
    btns.add(ok);
    btns.add(cancel);
    add(btns, BorderLayout.SOUTH);

    ok.addActionListener(e -> {
      try {
        Section s = (Section) sectionBox.getSelectedItem();
        Instructor i = (Instructor) instrBox.getSelectedItem();

        log.info("Assigning instructor userId={} to sectionId={}", 
                 i != null ? i.getUserId() : null,
                 s != null ? s.getSectionId() : null);

        svc.assignInstructor(
            s.getSectionId(),
            i.getUserId());

        log.info("Instructor assignment successful");
        UIError.info("Instructor assigned.");
        dispose();

      } catch (Exception ex) {
        log.error("Failed to assign instructor", ex);
        UIError.show(ex);
      }
    });

    cancel.addActionListener(e -> {
      log.debug("AssignInstructorDialog canceled");
      dispose();
    });

    pack();
    setLocationRelativeTo(getOwner());
    log.debug("AssignInstructorDialog initialized and displayed");
  }
}
