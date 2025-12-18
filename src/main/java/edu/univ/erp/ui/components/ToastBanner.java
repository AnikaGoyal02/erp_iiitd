package edu.univ.erp.ui.components;

import javax.swing.*;
import java.awt.*;

public class ToastBanner extends JPanel {

    private JLabel text = new JLabel("", SwingConstants.CENTER);

    public ToastBanner() {
        setLayout(new BorderLayout());
        setBackground(new Color(255, 230, 140));
        text.setFont(text.getFont().deriveFont(Font.BOLD, 14f));
        text.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(text, BorderLayout.CENTER);
        setVisible(false);
    }

    public void showToast(String msg) {
        text.setText(msg);
        setVisible(true);

        Timer hide = new Timer(5000, e -> setVisible(false));
        hide.setRepeats(false);
        hide.start();
    }
}
