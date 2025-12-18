package edu.univ.erp.data;

import edu.univ.erp.domain.Notification;
import edu.univ.erp.exception.DatabaseException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationDao extends BaseDao {
  private static final Logger log = LoggerFactory.getLogger(NotificationDao.class);

    public void insert(int senderId, String type, Integer targetId,
                       String title, String message) {

        log.debug("Inserting notification: senderId={} type={} targetId={} title={}",
                senderId, type, targetId, title);

        String sql = """
            INSERT INTO notifications 
            (sender_user_id, target_type, target_id, title, message)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection con = DBPool.erp().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, senderId);
            ps.setString(2, type);

            if (targetId == null) {
                log.trace("TargetId is NULL for notification insert");
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, targetId);
            }

            ps.setString(4, title);
            ps.setString(5, message);

            ps.executeUpdate();
            log.info("Notification inserted successfully for senderId={}", senderId);

        } catch (SQLException e) {
            log.error("Failed inserting notification for senderId={}", senderId, e);
            throw new DatabaseException("Failed inserting notification", e);
        }
    }


    public List<Notification> fetchForUser(
        String role, int userId,
        List<Integer> sectionIds,
        List<Integer> courseIds) {

        log.debug("Fetching notifications for userId={} role={} sectionIds={} courseIds={}",
                userId, role, sectionIds, courseIds);

        List<Notification> list = new ArrayList<>();

        String courseStr = courseIds.isEmpty()
                ? "0"
                : String.join(",", courseIds.stream().map(String::valueOf).toList());

        String sectionStr = sectionIds.isEmpty()
                ? "0"
                : String.join(",", sectionIds.stream().map(String::valueOf).toList());


                String sql = """
                    SELECT id AS notif_id,
                           sender_user_id,
                           target_type,
                           target_id,
                           title,
                           message,
                           created_at
                    FROM notifications
                    WHERE
                        sender_user_id <> ?
                        AND (
                               target_type='ALL'
                            OR (target_type='ALL_STUDENTS' AND ?='STUDENT')
                            OR (target_type='ALL_INSTRUCTORS' AND ?='INSTRUCTOR')
                            OR (target_type='COURSE'  AND target_id IN (%COURSES%))
                            OR (target_type='SECTION' AND target_id IN (%SECTIONS%))
                        )
                    ORDER BY created_at DESC
                    LIMIT 100
                """;

        sql = sql.replace("%COURSES%", courseStr);
        sql = sql.replace("%SECTIONS%", sectionStr);

        log.trace("Final SQL for notifications: {}", sql);

        try (Connection con = DBPool.erp().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int idx = 1;

            ps.setInt(idx++, userId);
            ps.setString(idx++, role);
            ps.setString(idx++, role);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Object tidObj = rs.getObject("target_id");
                Integer targetId = (tidObj == null) ? null : ((Number) tidObj).intValue();

                Notification n = new Notification(
                        rs.getInt("notif_id"),
                        rs.getInt("sender_user_id"),
                        null,
                        rs.getString("target_type"),
                        targetId,
                        rs.getString("title"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );

                log.trace("Loaded notification id={} title={}", n.getNotifId(), n.getTitle());
                list.add(n);
            }

            log.info("Loaded {} notifications for userId={}", list.size(), userId);

        } catch (SQLException e) {
            log.error("Failed loading notifications for userId={}", userId, e);
            throw new DatabaseException("Failed loading notifications", e);
        }

        return list;
    }
}
