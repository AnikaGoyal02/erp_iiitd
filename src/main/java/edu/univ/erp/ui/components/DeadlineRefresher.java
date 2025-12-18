package edu.univ.erp.ui.components;

import edu.univ.erp.data.SettingsDao;
import java.time.LocalDate;

import javax.swing.*;

public class DeadlineRefresher {

    private final Timer timer;

    public DeadlineRefresher(Runnable refreshFn) {
        timer = new Timer(3000, e -> refreshFn.run());
        timer.start();
    }

    public void stop() {
        timer.stop();
    }
}
