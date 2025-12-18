package edu.univ.erp.service;

import edu.univ.erp.auth.SessionManager;
import edu.univ.erp.data.*;
import edu.univ.erp.domain.*;
import edu.univ.erp.domain.grades.*;
import edu.univ.erp.exception.*;
import edu.univ.erp.util.AutoTableResize;

import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InstructorService {

    private static final Logger log = LoggerFactory.getLogger(InstructorService.class);

    private final AccessControl ac = new AccessControl();
    private final SectionDao sectionDao = new SectionDao();
    private final EnrollmentDao enrollmentDao = new EnrollmentDao();
    private final GradeDao gradeDao = new GradeDao();
    private final StudentDao studentDao = new StudentDao();

   
    public List<String[]> mySections() {
        log.debug("mySections(): entering");
        ac.requireRole(Role.INSTRUCTOR);
        Instructor inst = (Instructor) SessionManager.getCurrentUser();

        String sql = """
            SELECT s.section_id, c.code, c.title, s.semester, s.year, s.day_time, s.room, s.capacity
            FROM sections s
            JOIN courses c ON c.course_id = s.course_id
            WHERE s.instructor_id = (
                SELECT instructor_id FROM instructors WHERE user_id = ?
            )
            ORDER BY s.year DESC, s.semester
        """;

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] { "Section ID", "Code", "Title", "Sem", "Year", "Time", "Room", "Capacity" });

        try (var con = DBPool.erp().getConnection();
             var ps = con.prepareStatement(sql)) {

            ps.setInt(1, inst.getUserId());
            var rs = ps.executeQuery();

            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("section_id"),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("semester"),
                        rs.getString("year"),
                        rs.getString("day_time"),
                        rs.getString("room"),
                        rs.getString("capacity")
                });
            }


        } catch (SQLException e) {
            log.error("mySections(): Failed loading instructor sections", e);
            throw new DatabaseException("Failed loading instructor sections", e);
        }

        log.debug("mySections(): returning {} rows", rows.size());
        return rows;
    }

    
    public List<String[]> mySectionsRaw() {
        log.debug("mySectionsRaw(): entering");
        try {
            int userId = SessionManager.getCurrentUser().getUserId();

            String sql = """
                SELECT s.section_id, s.course_id
                FROM sections s
                JOIN instructors i ON s.instructor_id = i.instructor_id
                WHERE i.user_id = ?
            """;

            try (var con = DBPool.erp().getConnection();
                 var ps = con.prepareStatement(sql)) {

                ps.setInt(1, userId);
                var rs = ps.executeQuery();

                List<String[]> list = new java.util.ArrayList<>();

                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("section_id"),
                            rs.getString("course_id")
                    });
                }

                log.debug("mySectionsRaw(): returning {} rows", list.size());
                return list;
            }

        } catch (Exception e) {
            log.error("mySectionsRaw(): Failed loading instructor sections", e);
            throw new RuntimeException("Failed loading instructor sections", e);
        }
    }
        


    public List<Enrollment> sectionEnrollments(int sectionId) {
        log.debug("sectionEnrollments(): sectionId={}", sectionId);
    ac.requireRole(Role.INSTRUCTOR);
    Instructor inst = (Instructor) SessionManager.getCurrentUser();

    // verifying ownership
    int instId;
    String sqlGetInst = "SELECT instructor_id FROM instructors WHERE user_id = ?";
    try (var con = DBPool.erp().getConnection();
         var ps = con.prepareStatement(sqlGetInst)) {
        ps.setInt(1, inst.getUserId());
        var rs = ps.executeQuery();
        rs.next();
        instId = rs.getInt(1);
    } catch (Exception ex) {
        log.error("sectionEnrollments(): Instructor lookup failed for userId={}", inst.getUserId(), ex);
        throw new DatabaseException("Instructor lookup failed", ex);
    }

    Section sec = sectionDao.findById(sectionId);
    if (sec.getInstructorId() != instId) {
        log.warn("sectionEnrollments(): access denied for userId={} sectionId={}", inst.getUserId(), sectionId);
        throw new AccessDeniedException("Not your section.");
    }


    String sql = """
        SELECT e.enrollment_id,
               e.student_id,
               e.section_id,
               e.status,
               e.final_grade,
               s.roll_no,
               s.email
        FROM enrollments e
        JOIN students s ON s.student_id = e.student_id
        WHERE e.section_id = ?
    """;

    List<Enrollment> list = new ArrayList<>();

    try (var con = DBPool.erp().getConnection();
         var ps = con.prepareStatement(sql)) {

        ps.setInt(1, sectionId);
        var rs = ps.executeQuery();

        while (rs.next()) {
            Enrollment e = new Enrollment(
                rs.getInt("enrollment_id"),
                rs.getInt("student_id"),
                rs.getInt("section_id"),
                Enrollment.Status.valueOf(rs.getString("status")),
                null,
                null,
                rs.getString("final_grade")
            );

            e.setStudentRollNo(rs.getString("roll_no"));
            e.setStudentEmail(rs.getString("email"));

            list.add(e);
        }

    } catch (SQLException e) {
        log.error("sectionEnrollments(): Failed loading enrollments for sectionId={}", sectionId, e);
        throw new DatabaseException("Failed loading enrollments", e);
    }

    log.debug("sectionEnrollments(): returning {} enrollments for sectionId={}", list.size(), sectionId);
    return list;
    }



    
    public void addGradeComponent(int enrollmentId, GradeComponent gc) {
        log.debug("addGradeComponent(): enrollmentId={} component={}", enrollmentId, gc == null ? "null" : gc.getName());
        ac.requireRole(Role.INSTRUCTOR);
        ac.requireMaintenanceOff();
        Instructor inst = (Instructor) SessionManager.getCurrentUser();

        Enrollment e = enrollmentDao.findById(enrollmentId);
        Section sec = sectionDao.findById(e.getSectionId());


        int instructorId = getInstructorIdFromUser(inst.getUserId());

        if (!Objects.equals(sec.getInstructorId(), instructorId)) {
            log.warn("addGradeComponent(): Not your section. userId={} enrollmentId={}", inst.getUserId(), enrollmentId);
            throw new AccessDeniedException("Not your section.");
        }


        String sql = """
            INSERT INTO grades (enrollment_id, component_type, component_name, score, max_score, weightage)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (var con = DBPool.erp().getConnection();
             var ps = con.prepareStatement(sql)) {

            ps.setInt(1, enrollmentId);
            ps.setString(2, gc.getType().name());
            ps.setString(3, gc.getName());
            ps.setDouble(4, gc.getScore());
            ps.setDouble(5, gc.getMaxScore());
            ps.setDouble(6, gc.getWeightage());
            ps.executeUpdate();

        } catch (SQLException ex) {
            log.error("addGradeComponent(): Failed adding grade component for enrollmentId={}", enrollmentId, ex);
            throw new DatabaseException("Failed adding grade component", ex);
        }
        log.debug("addGradeComponent(): added component '{}' to enrollmentId={}", gc.getName(), enrollmentId);
    }


    public String computeFinalGrade(int enrollmentId) {
        log.debug("computeFinalGrade(): enrollmentId={}", enrollmentId);
        ac.requireRole(Role.INSTRUCTOR);
        ac.requireMaintenanceOff();

        Enrollment e = enrollmentDao.findById(enrollmentId);
        Section sec = sectionDao.findById(e.getSectionId());



        Instructor inst = (Instructor) SessionManager.getCurrentUser();

        int instructorId = getInstructorIdFromUser(inst.getUserId());
        if (!Objects.equals(sec.getInstructorId(), instructorId)) {
            log.warn("computeFinalGrade(): Not your section. userId={} enrollmentId={}", inst.getUserId(), enrollmentId);
            throw new AccessDeniedException("Not your section.");
        }


        List<GradeComponent> list = gradeDao.findByEnrollment(enrollmentId);

        double total = 0;
        for (GradeComponent gc : list) {
            total += gc.weightedScore();
        }

        log.debug("computeFinalGrade(): numeric total={} for enrollmentId={}", total, enrollmentId);

        String letter = toLetterGrade(total);

        try (var con = DBPool.erp().getConnection();
             var ps = con.prepareStatement(
                     "UPDATE enrollments SET final_grade=? WHERE enrollment_id=?")) {
            ps.setString(1, letter);
            ps.setInt(2, enrollmentId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("computeFinalGrade(): Failed updating final grade for enrollmentId={}", enrollmentId, ex);
            throw new DatabaseException("Failed updating final grade", ex);
        }

        log.info("computeFinalGrade(): computed final letter '{}' for enrollmentId={}", letter, enrollmentId);
        return letter;
    }

    private String toLetterGrade(double x) {
        if (x>=95) return "A+" ; 
        if (x>=90) return "A" ; 
        if (x >= 85) return "A-";
        if (x >= 75) return "B";
        if (x >= 65) return "B-";
        if (x >= 55) return "C";
        if (x >= 45) return "D";
        return "F";
    }

    
public List<String[]> exportSectionGrades(int sectionId) {
    log.debug("exportSectionGrades(): sectionId={}", sectionId);
    ac.requireRole(Role.INSTRUCTOR);

    Instructor inst = (Instructor) SessionManager.getCurrentUser();
    Section sec = sectionDao.findById(sectionId);

    int instructorId = getInstructorIdFromUser(inst.getUserId());
    if (!Objects.equals(sec.getInstructorId(), instructorId)) {
        throw new AccessDeniedException("Not your section.");
    }

    // Loading enrollments
    String sql = """
        SELECT e.enrollment_id,
               s.roll_no,
               s.email,
               e.status,
               e.final_grade
        FROM enrollments e
        JOIN students s ON s.student_id = e.student_id
        WHERE e.section_id=?
    """;

    List<Enrollment> enrollments = new ArrayList<>();

    try (var con = DBPool.erp().getConnection();
         var ps = con.prepareStatement(sql)) {

        ps.setInt(1, sectionId);
        var rs = ps.executeQuery();

        while (rs.next()) {
            Enrollment e = new Enrollment(
                    rs.getInt("enrollment_id"),
                    0, 0,
                    Enrollment.Status.valueOf(rs.getString("status")),
                    null, null,
                    rs.getString("final_grade")
            );
            e.setStudentRollNo(rs.getString("roll_no"));
            e.setStudentEmail(rs.getString("email"));
            enrollments.add(e);
        }
    } catch (SQLException ex) {
        throw new DatabaseException("Failed exporting grades", ex);
    }

    // Loading components
    Map<Integer, List<GradeComponent>> compsMap = getComponentsForSection(sectionId);

    // Ensuring Unique component names
    Set<String> componentNames = new TreeSet<>();
    compsMap.values().forEach(list -> list.forEach(gc -> componentNames.add(gc.getName())));


    List<String> header = new ArrayList<>();
    header.add("Enrollment ID");
    header.add("Roll No");
    header.add("Email");
    header.add("Status");
    header.add("Final Grade");

    for (String comp : componentNames) {
        header.add(comp + "_score");
        header.add(comp + "_max");
        header.add(comp + "_type");
        header.add(comp + "_weight");
    }

    List<String[]> output = new ArrayList<>();
    output.add(header.toArray(new String[0]));


    for (Enrollment e : enrollments) {
        List<String> row = new ArrayList<>();

        row.add("" + e.getEnrollmentId());
        row.add(e.getStudentRollNo());
        row.add(e.getStudentEmail());
        row.add(e.getStatus().name());
        row.add(e.getFinalGrade() == null ? "-" : e.getFinalGrade());


        Map<String, GradeComponent> studentMap = new HashMap<>();
        compsMap.getOrDefault(e.getEnrollmentId(), List.of())
                .forEach(gc -> studentMap.put(gc.getName(), gc));

        for (String comp : componentNames) {
            GradeComponent gc = studentMap.get(comp);

            if (gc == null) {
                row.add("-");
                row.add("-");
                row.add("-");
                row.add("-");
            } else {
                row.add("" + gc.getScore());
                row.add("" + gc.getMaxScore());
                row.add(gc.getType().name());
                row.add("" + gc.getWeightage());
            }
        }

        output.add(row.toArray(new String[0]));
    }

    return output;
}

private String clean(String s) {
    if (s == null) return "";
    s = s.trim();
    if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
        s = s.substring(1, s.length() - 1);
    }
    return s.trim();
}



public void importGradesFromCSV(int sectionId, Path csvPath) throws Exception {
    log.debug("importGradesFromCSV(): sectionId={} csvPath={}", sectionId, csvPath);
    ac.requireRole(Role.INSTRUCTOR);
    ac.requireMaintenanceOff();

    Instructor inst = (Instructor) SessionManager.getCurrentUser();
    Section sec = sectionDao.findById(sectionId);

    int instructorId = getInstructorIdFromUser(inst.getUserId());
    if (!Objects.equals(sec.getInstructorId(), instructorId)) {
        throw new AccessDeniedException("Not your section.");
    }

    List<String> lines = Files.readAllLines(csvPath);
    if (lines.isEmpty()) throw new ValidationException("CSV is empty.");

    String[] headers = lines.get(0).trim().split(",");

    // Ensuring correct layout
    if ((headers.length - 5) % 4 != 0) {
        throw new ValidationException("Invalid CSV format: components must be groups of 4 columns.");
    }

    int compCount = (headers.length - 5) / 4;
    List<String> compNames = new ArrayList<>();

    for (int i = 0; i < compCount; i++) {
        String h = clean(headers[5 + i * 4]); 
        String comp = h.substring(0, h.lastIndexOf("_score"));
        compNames.add(comp);
    }

    
    Set<Integer> validEnrollments = new HashSet<>();
    try (var con = DBPool.erp().getConnection();
         var ps = con.prepareStatement("SELECT enrollment_id FROM enrollments WHERE section_id=?")) {

        ps.setInt(1, sectionId);
        var rs = ps.executeQuery();
        while (rs.next()) validEnrollments.add(rs.getInt(1));
    }

   
    try (var con = DBPool.erp().getConnection();
         var ps = con.prepareStatement("""
            DELETE g FROM grades g
            JOIN enrollments e ON g.enrollment_id=e.enrollment_id
            WHERE e.section_id=?
         """)) {
        ps.setInt(1, sectionId);
        ps.executeUpdate();
    }

    // Each csv row
    for (int i = 1; i < lines.size(); i++) {
        String line = lines.get(i).trim();
        if (line.isBlank()) continue;

        String[] parts = line.split(",");

        int enrollmentId = Integer.parseInt(clean(parts[0]));
        if (!validEnrollments.contains(enrollmentId)) continue;

        
        String status = clean(parts[3]);
        String finalGrade = clean(parts[4]);

        try (var con = DBPool.erp().getConnection();
             var ps = con.prepareStatement(
                     "UPDATE enrollments SET status=?, final_grade=? WHERE enrollment_id=?")) {

            ps.setString(1, status.equals("-") ? null : status);
            ps.setString(2, finalGrade.equals("-") ? null : finalGrade);
            ps.setInt(3, enrollmentId);
            ps.executeUpdate();
        }

        // Inserting grade compo
        for (int c = 0; c < compCount; c++) {

            String compName = compNames.get(c);

            String scoreStr = clean(parts[5 + c * 4]);
            String maxStr   = clean(parts[6 + c * 4]);
            String typeStr  = clean(parts[7 + c * 4]);
            String wtStr    = clean(parts[8 + c * 4]);

            // Skiping missing component values
            if (scoreStr.equals("-") || maxStr.equals("-") || typeStr.equals("-") || wtStr.equals("-"))
                continue;

            double score, max, weight;

            try {
                score = Double.parseDouble(scoreStr);
                max   = Double.parseDouble(maxStr);
                weight = Double.parseDouble(wtStr);
            } catch (NumberFormatException ex) {
                log.warn("Skipping invalid numeric component at row {}: {}, {}, {}", i+1, scoreStr, maxStr, wtStr);
                continue;
            }

            try (var con = DBPool.erp().getConnection();
                 var ps = con.prepareStatement("""
                    INSERT INTO grades (enrollment_id, component_type, component_name, score, max_score, weightage)
                    VALUES (?, ?, ?, ?, ?, ?)
                 """)) {

                ps.setInt(1, enrollmentId);
                ps.setString(2, typeStr);
                ps.setString(3, compName);
                ps.setDouble(4, score);
                ps.setDouble(5, max);
                ps.setDouble(6, weight);

                ps.executeUpdate();
            }
        }
    }

    log.info("IMPORT COMPLETED — SAFE RESTORE MODE for sectionId={}", sectionId);
}



private int getInstructorIdFromUser(int userId) {
    log.debug("getInstructorIdFromUser(): userId={}", userId);

    String sql = "SELECT instructor_id FROM instructors WHERE user_id = ?";

    try (var con = DBPool.erp().getConnection();
         var ps = con.prepareStatement(sql)) {

        ps.setInt(1, userId);
        var rs = ps.executeQuery();

        if (rs.next())
            return rs.getInt(1);

        throw new NotFoundException("Instructor record not found for user " + userId);

    } catch (SQLException ex) {
        log.error("getInstructorIdFromUser(): failed for userId={}", userId, ex);
        throw new DatabaseException("Failed to resolve instructor_id", ex);
    }
}


public Map<Integer, List<GradeComponent>> getComponentsForSection(int sectionId) {
    log.debug("getComponentsForSection(): sectionId={}", sectionId);

    String sql = """
        SELECT g.enrollment_id,
               g.component_name,
               g.score,
               g.max_score,
               g.weightage,
               g.component_type
        FROM grades g
        JOIN enrollments e ON e.enrollment_id = g.enrollment_id
        WHERE e.section_id = ?
        ORDER BY g.component_name
    """;

    Map<Integer, List<GradeComponent>> map = new HashMap<>();

    try (var con = DBPool.erp().getConnection();
         var ps = con.prepareStatement(sql)) {

        ps.setInt(1, sectionId);
        var rs = ps.executeQuery();

        while (rs.next()) {
            int eid = rs.getInt("enrollment_id");

            GradeComponent gc = switch (GradeType.valueOf(rs.getString("component_type"))) {
                case QUIZ       -> new QuizComponent(rs.getString("component_name"), rs.getDouble("score"), rs.getDouble("max_score"), rs.getDouble("weightage"));
                case MIDSEM     -> new MidsemComponent(rs.getString("component_name"), rs.getDouble("score"), rs.getDouble("max_score"), rs.getDouble("weightage"));
                case ENDSEM     -> new EndsemComponent(rs.getString("component_name"), rs.getDouble("score"), rs.getDouble("max_score"), rs.getDouble("weightage"));
                case ASSIGNMENT -> new AssignmentComponent(rs.getString("component_name"), rs.getDouble("score"), rs.getDouble("max_score"), rs.getDouble("weightage"));
                case PROJECT    -> new ProjectComponent(rs.getString("component_name"), rs.getDouble("score"), rs.getDouble("max_score"), rs.getDouble("weightage"));
            };

            map.computeIfAbsent(eid, k -> new ArrayList<>()).add(gc);
        }

    } catch (Exception e) {
        log.error("getComponentsForSection(): failed for sectionId={}", sectionId, e);
        throw new DatabaseException("Failed to load grade components", e);
    }

    return map;
}


public Map<String, Double> classStats(int sectionId) {
    log.debug("classStats(): sectionId={}", sectionId);
    ac.requireRole(Role.INSTRUCTOR);

    Instructor inst = (Instructor) SessionManager.getCurrentUser();
    Section sec = sectionDao.findById(sectionId);

    int instructorId = getInstructorIdFromUser(inst.getUserId());
    if (!Objects.equals(sec.getInstructorId(), instructorId)) {
        log.warn("classStats(): access denied for userId={} sectionId={}", inst.getUserId(), sectionId);
        throw new AccessDeniedException("Not your section.");
    }

    //  Load all enrollments 
    String enrollSql = """
        SELECT enrollment_id
        FROM enrollments
        WHERE section_id = ?
    """;

    List<Integer> enrollmentIds = new ArrayList<>();

    try (var con = DBPool.erp().getConnection();
         var ps = con.prepareStatement(enrollSql)) {

        ps.setInt(1, sectionId);
        var rs = ps.executeQuery();

        while (rs.next()) {
            enrollmentIds.add(rs.getInt("enrollment_id"));
        }

    } catch (SQLException ex) {
        log.error("classStats(): Failed loading enrollments for sectionId={}", sectionId, ex);
        throw new DatabaseException("Failed loading enrollments", ex);
    }

 
    Map<Integer, List<GradeComponent>> allComps = getComponentsForSection(sectionId);

    class CompDef {
        double maxScore;
        double weight;
    }

    Map<String, CompDef> compDefs = new HashMap<>();

    for (var entry : allComps.entrySet()) {
        for (GradeComponent gc : entry.getValue()) {
            String name = gc.getName();

            compDefs.compute(name, (k, old) -> {
                if (old == null) {
                    CompDef d = new CompDef();
                    d.maxScore = gc.getMaxScore();
                    d.weight = gc.getWeightage();
                    return d;
                } else {
                  
                    if (gc.getWeightage() > old.weight) {
                        old.maxScore = gc.getMaxScore();
                        old.weight = gc.getWeightage();
                    }
                    return old;
                }
            });
        }
    }

    
    List<Double> finalScores = new ArrayList<>();

    for (int eid : enrollmentIds) {
        List<GradeComponent> comps = gradeDao.findByEnrollment(eid);

        Map<String, GradeComponent> studentMap = new HashMap<>();
        for (GradeComponent gc : comps) {
            studentMap.put(gc.getName(), gc);
        }

        double total = 0;

        for (var entry : compDefs.entrySet()) {
            String name = entry.getKey();
            CompDef def = entry.getValue();

            GradeComponent gc = studentMap.get(name);

            if (gc == null) {
                total += 0;
            } else {
                total += (gc.getScore() / def.maxScore) * def.weight;
            }
        }

        finalScores.add(total);
    }

    // 4. Compute statistics
    Map<String, Double> out = new HashMap<>();
    if (finalScores.isEmpty()) {
        out.put("average", 0.0);
        out.put("min", 0.0);
        out.put("max", 0.0);
        return out;
    }

    double avg = finalScores.stream().mapToDouble(x -> x).average().orElse(0);
    double min = finalScores.stream().mapToDouble(x -> x).min().orElse(0);
    double max = finalScores.stream().mapToDouble(x -> x).max().orElse(0);

    out.put("average", avg);
    out.put("min", min);
    out.put("max", max);

    return out;
    }
}