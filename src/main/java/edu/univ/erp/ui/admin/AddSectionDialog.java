package edu.univ.erp.ui.admin;

import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.ui.UIError;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddSectionDialog extends JDialog {

  private static final Logger log = LoggerFactory.getLogger(AddSectionDialog.class);

  private final AdminService svc = new AdminService();

  public AddSectionDialog(Window owner) {
    super(owner, "Create Section", ModalityType.APPLICATION_MODAL);
    log.debug("Opening AddSectionDialog");
    init();
  }

  private void init() {
    log.debug("Initializing AddSectionDialog UI components");
    List<Course> courses = svc.listCourses();
    List<Instructor> instructors = svc.listInstructors();
    log.info("Loaded {} courses and {} instructors for section creation",
             courses.size(), instructors.size());

    setLayout(new BorderLayout(10, 10));
    JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
    JComboBox<Course> courseBox = new JComboBox<>(courses.toArray(new Course[0]));
    JComboBox<Instructor> instrBox = new JComboBox<>();
    instrBox.addItem(null);
    for (Instructor i : instructors)
      instrBox.addItem(i);

    JTextField dayTime = new JTextField(20);
    JTextField room = new JTextField(10);
    JTextField capacity = new JTextField(5);
    JTextField semester = new JTextField(10);
    JTextField year = new JTextField(5);

    form.add(new JLabel("Course:"));
    form.add(courseBox);
    form.add(new JLabel("Instructor (optional):"));
    form.add(instrBox);
    form.add(new JLabel("Day/Time:"));
    form.add(dayTime);
    form.add(new JLabel("Room:"));
    form.add(room);
    form.add(new JLabel("Capacity:"));
    form.add(capacity);
    form.add(new JLabel("Semester:"));
    form.add(semester);
    form.add(new JLabel("Year:"));
    form.add(year);
    add(form, BorderLayout.CENTER);

    JPanel btns = new JPanel();
    JButton ok = new JButton("Create");
    JButton cancel = new JButton("Cancel");
    btns.add(ok);
    btns.add(cancel);
    add(btns, BorderLayout.SOUTH);

    ok.addActionListener(e -> {
      Course c = (Course) courseBox.getSelectedItem();
      Instructor inst = (Instructor) instrBox.getSelectedItem();

      log.info("Attempting to create section: courseId={}, instructorUserId={}, dayTime={}, room={}, capacity={}, semester={}, year={}",
              c != null ? c.getCourseId() : null,
              inst != null ? inst.getUserId() : null,
              dayTime.getText().trim(),
              room.getText().trim(),
              capacity.getText().trim(),
              semester.getText().trim(),
              year.getText().trim());

      try {
        svc.createSection(
            c.getCourseId(),
            inst != null ? inst.getUserId() : null,
            dayTime.getText().trim(),
            room.getText().trim(),
            Integer.parseInt(capacity.getText().trim()),
            semester.getText().trim(),
            Integer.parseInt(year.getText().trim()));

        log.info("Section creation successful");
        UIError.info("Section created.");
        dispose();
      } catch (Exception ex) {
        log.error("Section creation failed", ex);
        UIError.show(ex);
      }
    });

    cancel.addActionListener(e -> {
      log.debug("AddSectionDialog canceled");
      dispose();
    });

    pack();
    setLocationRelativeTo(getOwner());
    log.debug("AddSectionDialog ready and displayed");
  }
}
