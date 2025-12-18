package edu.univ.erp.ui.admin;

import edu.univ.erp.domain.Role;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.ui.UIError;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddUserDialog extends JDialog {

  private static final Logger log = LoggerFactory.getLogger(AddUserDialog.class);

  private final AdminService svc = new AdminService();

  public AddUserDialog(Window owner) {
    super(owner, "Create User", ModalityType.APPLICATION_MODAL);
    log.debug("Opening AddUserDialog");
    init();
  }

  private void init() {
    log.debug("Initializing AddUserDialog UI components");

    setLayout(new BorderLayout(10, 10));

    JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

    JTextField username = new JTextField(15);
    JPasswordField password = new JPasswordField(15);
    JComboBox<Role> roleBox = new JComboBox<>(Role.values());

    
    JTextField roll = new JTextField(10);
    JTextField program = new JTextField(10);
    JTextField year = new JTextField(4);
    JTextField stuEmail = new JTextField(15);

    JPanel studentPanel = new JPanel(new GridLayout(0, 2, 8, 8));
    studentPanel.add(new JLabel("Roll No:"));
    studentPanel.add(roll);
    studentPanel.add(new JLabel("Program:"));
    studentPanel.add(program);
    studentPanel.add(new JLabel("Year (1-4):"));
    studentPanel.add(year);
    studentPanel.add(new JLabel("Email:"));
    studentPanel.add(stuEmail);
    studentPanel.setVisible(false);

    JTextField department = new JTextField(10);
    JTextField instEmail = new JTextField(15);

    JPanel instructorPanel = new JPanel(new GridLayout(0, 2, 8, 8));
    instructorPanel.add(new JLabel("Department:"));
    instructorPanel.add(department);
    instructorPanel.add(new JLabel("Email:"));
    instructorPanel.add(instEmail);
    instructorPanel.setVisible(false);

    // Role change listener
    roleBox.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        Role r = (Role) roleBox.getSelectedItem();
        studentPanel.setVisible(r == Role.STUDENT);
        instructorPanel.setVisible(r == Role.INSTRUCTOR);
        pack();
      }
    });

    form.add(new JLabel("Username:"));
    form.add(username);
    form.add(new JLabel("Password:"));
    form.add(password);
    form.add(new JLabel("Role:"));
    form.add(roleBox);

    add(form, BorderLayout.NORTH);
    add(studentPanel, BorderLayout.CENTER);
    add(instructorPanel, BorderLayout.SOUTH);

    // Buttons
    JPanel buttons = new JPanel();
    JButton ok = new JButton("Create");
    JButton cancel = new JButton("Cancel");
    buttons.add(ok);
    buttons.add(cancel);
    add(buttons, BorderLayout.PAGE_END);


    ok.addActionListener(e -> {
      String u = username.getText().trim();
      String p = new String(password.getPassword()).trim();
      Role role = (Role) roleBox.getSelectedItem();

      log.info("Attempting to create user: username={} role={}", u, role);

      try {
        // Call new unified method
        svc.createUserWithProfile(
            u,
            p,
            role,
            // student fields
            roll.getText().trim(),
            program.getText().trim(),
            year.getText().trim(),
            stuEmail.getText().trim(),
            // instructor fields
            department.getText().trim(),
            instEmail.getText().trim()
        );

        UIError.info("User created successfully!");
        log.info("User successfully created: {}", u);
        dispose();

      } catch (Exception ex) {
        log.error("User creation failed: {}", ex.getMessage(), ex);
        UIError.show(ex);
      }
    });

    cancel.addActionListener(e -> dispose());

    pack();
    setLocationRelativeTo(getOwner());
    log.debug("AddUserDialog ready and displayed");
  }
}
