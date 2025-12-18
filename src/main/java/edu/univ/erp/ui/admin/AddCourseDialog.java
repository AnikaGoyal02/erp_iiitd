package edu.univ.erp.ui.admin;

import edu.univ.erp.service.AdminService;
import edu.univ.erp.ui.UIError;

import javax.swing.*;
import java.awt.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddCourseDialog extends JDialog {
  private static final Logger log = LoggerFactory.getLogger(AddCourseDialog.class);

  private final AdminService svc = new AdminService();

  public AddCourseDialog(Window owner) {
    super(owner, "Create Course", ModalityType.APPLICATION_MODAL);
    log.debug("Opening AddCourseDialog");
    init();
  }

  private void init() {
    log.debug("Initializing AddCourseDialog UI components");

    setLayout(new BorderLayout(10, 10));

    JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

    JTextField code = new JTextField(12);
    JTextField title = new JTextField(20);
    JTextField credits = new JTextField(3);


    JTextArea description = new JTextArea(4, 20);
    description.setLineWrap(true);
    description.setWrapStyleWord(true);
    JScrollPane descScroll = new JScrollPane(description);

    form.add(new JLabel("Course Code:"));
    form.add(code);

    form.add(new JLabel("Title:"));
    form.add(title);

    form.add(new JLabel("Description:"));
    form.add(descScroll);

    form.add(new JLabel("Credits:"));
    form.add(credits);

    add(form, BorderLayout.CENTER);

    JPanel btns = new JPanel();
    JButton ok = new JButton("Create");
    JButton cancel = new JButton("Cancel");
    btns.add(ok);
    btns.add(cancel);

    add(btns, BorderLayout.SOUTH);

    ok.addActionListener(e -> {
      String c = code.getText().trim();
      String t = title.getText().trim();
      String d = description.getText().trim();
      String cr = credits.getText().trim();

      log.info("Attempting to create course: code={}, title={}, credits={}", c, t, cr);

      try {
        int creditValue = Integer.parseInt(cr);

        svc.createCourse(c,t,d,creditValue);

        log.info("Course creation successful");
        UIError.info("Course created.");
        dispose();

      } catch (Exception ex) {
        log.error("Course creation failed", ex);
        UIError.show(ex);
      }
    });

    cancel.addActionListener(e -> {
      log.debug("AddCourseDialog canceled");
      dispose();
    });

    pack();
    setLocationRelativeTo(getOwner());
    log.debug("AddCourseDialog initialized and ready");
  }
}
