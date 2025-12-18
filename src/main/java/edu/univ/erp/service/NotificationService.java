package edu.univ.erp.service;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.NotificationDao;
import edu.univ.erp.domain.*;
import edu.univ.erp.exception.AccessDeniedException;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationDao dao = new NotificationDao();
    private static int lastMaxId = 0;

    // send notification
    public void sendNotification(String type, Integer targetId,
                                 String title, String message) {

        User u = SessionManager.getCurrentUser();
        log.info("Sending notification: type={}, targetId={}, title={}, fromUserId={}",
                type, targetId, title, u.getUserId());

        if (u instanceof Student) {
            log.warn("Access denied: Student userId={} attempted to send a notification", u.getUserId());
            throw new AccessDeniedException("Students cannot send notifications.");
        }

        if (u instanceof Instructor) {
            if (!(type.equals("STUDENTS") ||
                  type.equals("COURSE") ||
                  type.equals("SECTION"))) {
                log.warn("Access denied: Instructor userId={} attempted invalid target type={}",
                        u.getUserId(), type);
                throw new AccessDeniedException("Instructors cannot send to this target.");
            }
        }

        dao.insert(u.getUserId(), type, targetId, title, message);
        log.info("Notification sent successfully by userId={}", u.getUserId());
    }


    public List<Notification> getMyNotifications() {

        User u = SessionManager.getCurrentUser();
        log.debug("Fetching notifications for userId={}, role={}", u.getUserId(), u.getRole());

        if (u instanceof Admin) {
            log.debug("Admin userId={} receives no notifications", u.getUserId());
            return List.of(); // Admin never receives
        }

        String role = u.getRole().name();

        List<Integer> sections = Collections.emptyList();
        List<Integer> courses = Collections.emptyList();

        if (u instanceof Student) {
            log.debug("Resolving student sections for userId={}", u.getUserId());
            var reg = new StudentService().myRegisteredSections();
            sections = reg.stream().map(Section::getSectionId).toList();
            courses  = reg.stream().map(Section::getCourseId).toList();
        }

        if (u instanceof Instructor) {
            log.debug("Resolving instructor sections for userId={}", u.getUserId());
            var my = new InstructorService().mySectionsRaw();
            sections = my.stream().map(arr -> Integer.parseInt(arr[0])).toList();
            courses  = my.stream().map(arr -> Integer.parseInt(arr[1])).toList();
        }

        List<Notification> list = dao.fetchForUser(role, u.getUserId(), sections, courses);
        log.info("Fetched {} notifications for userId={}", list.size(), u.getUserId());

        if (!list.isEmpty() && list.get(0).getNotifId() > lastMaxId) {
            int old = lastMaxId;
            lastMaxId = list.get(0).getNotifId();
            log.info("New notification detected for userId={}, lastMaxId {} -> {}", 
                     u.getUserId(), old, lastMaxId);
            NotificationListener.fire();
        }

        return list;
    }
}
