package edu.univ.erp.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private static final List<Runnable> listeners = new ArrayList<>();

    public static void register(Runnable r) {
        log.info("Registering new notification listener: {}", r);
        listeners.add(r);
    }

    
    public static void fire() {
        log.info("Firing {} notification listeners", listeners.size());
        for (Runnable r : listeners) {
            try {
                log.debug("Executing listener {}", r);
                r.run();
            } catch (Exception ignored) {
                log.error("Listener threw exception but was ignored: {}", r, ignored);
            }
        }
        log.info("All listeners executed");
    }
}
