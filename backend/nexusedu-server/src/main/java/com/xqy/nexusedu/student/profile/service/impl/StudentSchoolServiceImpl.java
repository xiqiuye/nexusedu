package com.xqy.nexusedu.student.profile.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementAccessDeniedException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementConflictException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.student.profile.mapper.StudentSchoolMapper;
import com.xqy.nexusedu.system.authentication.mapper.SysAuthorizationMapper;

/** School-scoped student, campus and classroom use cases. */
@Service
public class StudentSchoolServiceImpl implements com.xqy.nexusedu.student.profile.service.StudentSchoolService {
    private final StudentSchoolMapper school;
    private final SysAuthorizationMapper roles;
    private final AuthorizationPolicy authorization;
    private final TransactionTemplate transactions;

    public StudentSchoolServiceImpl(StudentSchoolMapper school, SysAuthorizationMapper roles,
                                    AuthorizationPolicy authorization, PlatformTransactionManager transactionManager) {
        this.school = school;
        this.roles = roles;
        this.authorization = authorization;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public List<Map<String, Object>> campuses(InternalCallContext caller) {
        requireSchoolUser(caller);
        if (isSchoolAdmin(caller)) return school.findCampuses(caller.tenantId());
        if (roles.findRoleCodes(caller.userId()).contains("R_CAMPUS_ADMIN")) return school.findCampusesForAdmin(caller.tenantId(), caller.userId());
        return school.findCampusesForHomeroom(caller.tenantId(), caller.userId());
    }

    public Map<String, Object> createCampus(Map<String, Object> input, InternalCallContext caller) {
        requireSuper(caller);
        String code = required(input, "campusCode", 32), name = required(input, "name", 128);
        school.insertCampus(caller.tenantId(), code, name, text(input.get("address"), 255));
        return school.findCampus(caller.tenantId(), code);
    }

    public List<Map<String, Object>> academicYears(InternalCallContext caller) {
        requireSchoolUser(caller);
        return school.findAcademicYears(caller.tenantId());
    }

    @Transactional
    public Map<String, Object> createAcademicYear(Map<String, Object> input, InternalCallContext caller) {
        requireSuper(caller);
        String name = required(input, "name", 32);
        LocalDate start = LocalDate.parse(required(input, "startsOn", 10));
        LocalDate end = LocalDate.parse(required(input, "endsOn", 10));
        if (!end.isAfter(start)) throw new IllegalArgumentException("academic year end must be after start");
        boolean current = Boolean.parseBoolean(String.valueOf(input.getOrDefault("current", false)));
        if (current) school.clearCurrentAcademicYears(caller.tenantId());
        school.insertAcademicYear(caller.tenantId(), name, start, end, current);
        return school.findAcademicYear(caller.tenantId(), name);
    }

    public List<Map<String, Object>> classes(Long yearId, Long campusId, InternalCallContext caller) {
        requireSchoolUser(caller);
        return isSchoolAdmin(caller) ? school.findClasses(caller.tenantId(), yearId, campusId) : school.findClassesForUser(caller.tenantId(), yearId, campusId, caller.userId());
    }

    public Map<String, Object> createClass(Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        long campus = number(input, "campusId"), year = number(input, "academicYearId"), grade = number(input, "grade");
        validateGrade(grade);
        requireCampusAccess(caller, campus, true);
        if (school.countAcademicYear(caller.tenantId(), year) == 0) throw new ManagementNotFoundException();
        String name = required(input, "className", 64);
        school.insertClass(caller.tenantId(), campus, year, grade, name, input.get("capacity"));
        return school.findClass(caller.tenantId(), campus, year, grade, name);
    }

    public PageResult students(Map<String, String> filter, int current, int size, InternalCallContext caller) {
        requireSchoolUser(caller);
        filter = filter == null ? Map.of() : filter;
        current = Math.max(1, current); size = Math.max(1, Math.min(200, size));
        Long campus = optionalLong(filter.get("campusId")), year = optionalLong(filter.get("academicYearId")), grade = optionalLong(filter.get("grade")), classId = optionalLong(filter.get("classId"));
        String search = filter.getOrDefault("search", "").trim(), status = filter.getOrDefault("status", "").trim();
        String sortColumn = switch (filter.getOrDefault("sortBy", "studentNo")) { case "name" -> "s.name"; case "grade" -> "e.grade"; case "className" -> "c.class_name"; case "campusName" -> "p.name"; case "enrolledOn" -> "e.starts_on"; default -> "s.student_no"; };
        String sortDirection = "desc".equalsIgnoreCase(filter.get("sortDirection")) ? "DESC" : "ASC";
        List<String> userRoles = roles.findRoleCodes(caller.userId());
        boolean schoolAdmin = userRoles.contains("R_SUPER") || userRoles.contains("R_SCHOOL_ADMIN"), campusAdmin = userRoles.contains("R_CAMPUS_ADMIN");
        boolean useCurrentYear = year == null && school.countCurrentAcademicYear(caller.tenantId()) > 0;
        Long total = school.countStudents(caller.tenantId(), campus, year, grade, classId, status, search, campusAdmin, schoolAdmin, caller.userId(), useCurrentYear);
        List<Map<String, Object>> records = school.findStudents(caller.tenantId(), campus, year, grade, classId, status, search, campusAdmin, schoolAdmin, caller.userId(), useCurrentYear, sortColumn, sortDirection, size, (long) (current - 1) * size);
        return new PageResult(records, current, size, total == null ? 0 : total);
    }

    @Transactional
    public Map<String, Object> createStudent(Map<String, Object> input, InternalCallContext caller) {
        requireSchoolUser(caller);
        long campus = number(input, "campusId"), year = number(input, "academicYearId"), grade = number(input, "grade");
        requireCampusAccess(caller, campus, true); validateGrade(grade);
        String no = required(input, "studentNo", 64), name = required(input, "name", 128); Long classId = optionalLong(input.get("classId"));
        if (classId != null && school.countActiveClass(caller.tenantId(), classId, campus, year, grade) == 0) throw new IllegalArgumentException("class does not match campus, academic year or grade");
        String email = text(input.get("email"), 128), phone = text(input.get("phone"), 32);
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("invalid email format");
        if (phone != null && !phone.matches("^[0-9+() -]{6,32}$")) throw new IllegalArgumentException("invalid phone format");
        if (school.countStudentNo(caller.tenantId(), no) > 0) throw new ManagementConflictException("student number already exists");
        school.insertStudent(caller.tenantId(), no, name, text(input.get("gender"), 16), input.get("birthday"), email, phone);
        Long id = school.findStudentId(caller.tenantId(), no);
        school.insertEnrollment(caller.tenantId(), id, year, campus, grade, classId, "ENROLLED", LocalDate.now(), null, null, caller.userId());
        return school.findStudent(caller.tenantId(), id);
    }

    public Map<String, Object> importStudents(List<Map<String, Object>> input, InternalCallContext caller) {
        requireSchoolAdmin(caller); if (input == null) throw new IllegalArgumentException("import data cannot be null");
        int imported = 0; List<Map<String, Object>> errors = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            Map<String, Object> row = input.get(i);
            try {
                transactions.execute(status -> {
                    Map<String, Object> values = new HashMap<>(row);
                    if (values.get("campusId") == null) values.put("campusId", lookup(school.findCampusIdByName(caller.tenantId(), required(values, "campusName", 128))));
                    if (values.get("academicYearId") == null) values.put("academicYearId", lookup(school.findAcademicYearIdByName(caller.tenantId(), required(values, "academicYear", 32))));
                    if (values.get("grade") == null) values.put("grade", values.get("gradeNo"));
                    if (values.get("classId") == null && values.get("className") != null && !values.get("className").toString().isBlank()) values.put("classId", lookup(school.findClassId(caller.tenantId(), optionalLong(values.get("campusId")), optionalLong(values.get("academicYearId")), optionalLong(values.get("grade")), values.get("className").toString())));
                    createStudent(values, caller); return null;
                });
                imported++;
            } catch (Exception e) {
                errors.add(Map.of("row", i + 2, "studentNo", String.valueOf(row == null ? "" : row.getOrDefault("studentNo", "")), "message", e.getMessage() == null ? "import failed" : e.getMessage()));
            }
        }
        return Map.of("imported", imported, "total", input.size(), "errors", errors);
    }

    @Transactional
    public void moveStudent(Long studentId, Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller); Long enrollment = optionalLong(input.get("enrollmentId")); if (enrollment == null) throw new IllegalArgumentException("enrollment record is required");
        Map<String, Object> old = school.findEnrollmentForMove(caller.tenantId(), studentId, enrollment); if (old == null) throw new ManagementNotFoundException();
        requireCampusAccess(caller, number(old.get("campusId")), true);
        long campus = number(input, "campusId"), grade = number(input, "grade"); Long classId = optionalLong(input.get("classId")); requireCampusAccess(caller, campus, true); validateGrade(grade);
        if (classId != null && school.countTargetClass(caller.tenantId(), classId, campus, number(old.get("yearId")), grade) == 0) throw new IllegalArgumentException("target class does not match");
        String reason = text(input.get("reason"), 255); LocalDate effective = dateOrToday(input.get("effectiveOn"));
        school.updateEnrollmentTransfer(caller.tenantId(), enrollment, effective, reason, caller.userId());
        school.insertEnrollmentTransfer(caller.tenantId(), studentId, number(old.get("yearId")), campus, grade, classId, effective, reason, caller.userId());
    }

    @Transactional
    public void changeEnrollmentStatus(long studentId, Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller); Long enrollment = optionalLong(input.get("enrollmentId")); String status = required(input, "status", 20);
        if (!List.of("SUSPENDED", "ENROLLED", "TRANSFERRED", "GRADUATED").contains(status)) throw new IllegalArgumentException("unsupported enrollment status");
        Map<String, Object> old = school.findEnrollmentForStatus(caller.tenantId(), studentId, enrollment); if (old == null) throw new ManagementNotFoundException();
        requireCampusAccess(caller, number(old.get("campusId")), true); LocalDate effective = dateOrToday(input.get("effectiveOn"));
        if (effective.isBefore(toLocalDate(old.get("startsOn")))) throw new IllegalArgumentException("effective date cannot be before enrollment start");
        if (status.equals(old.get("status"))) throw new IllegalArgumentException("enrollment status is unchanged");
        String reason = text(input.get("reason"), 255);
        school.updateEnrollmentStatus(caller.tenantId(), enrollment, effective, reason, caller.userId());
        school.insertEnrollmentStatus(caller.tenantId(), studentId, number(old.get("yearId")), number(old.get("campusId")), number(old.get("grade")), optionalLong(old.get("classId")), status, effective, reason, caller.userId());
    }

    public void assignHomeroom(long classId, long userId, LocalDate start, InternalCallContext caller) {
        requireSchoolAdmin(caller); Long campus = school.findClassCampusId(caller.tenantId(), classId); if (campus == null) throw new ManagementNotFoundException();
        requireCampusAccess(caller, campus, true); requireTenantUser(userId, caller); assignRole(userId, "R_HOMEROOM");
        school.closeHomeroom(caller.tenantId(), classId, start.minusDays(1)); school.insertHomeroom(caller.tenantId(), classId, userId, start);
    }

    public void assignCampusAdmin(long campusId, long userId, InternalCallContext caller) {
        requireSuper(caller); requireCampusAccess(caller, campusId, true); requireTenantUser(userId, caller); assignRole(userId, "R_CAMPUS_ADMIN"); school.insertCampusAdmin(caller.tenantId(), campusId, userId);
    }

    public void assignTeacher(long classId, long userId, InternalCallContext caller) {
        requireSchoolAdmin(caller); Long campus = school.findClassCampusId(caller.tenantId(), classId); if (campus == null) throw new ManagementNotFoundException();
        requireCampusAccess(caller, campus, true); requireTenantUser(userId, caller); assignRole(userId, "R_TEACHER"); school.insertClassScope(caller.tenantId(), classId, userId);
    }

    private void requireSchoolUser(InternalCallContext c) { authorization.authorize("student:read", c); if (roles.findRoleCodes(c.userId()).stream().noneMatch(r -> List.of("R_SUPER", "R_CAMPUS_ADMIN", "R_HOMEROOM", "R_TEACHER").contains(r))) throw new ManagementAccessDeniedException(); }
    private void requireSchoolAdmin(InternalCallContext c) { authorization.authorize("student:write", c); if (roles.findRoleCodes(c.userId()).stream().noneMatch(r -> List.of("R_SUPER", "R_SCHOOL_ADMIN", "R_CAMPUS_ADMIN").contains(r))) throw new ManagementAccessDeniedException(); }
    private void requireSuper(InternalCallContext c) { authorization.authorize("student:write", c); if (!isSchoolAdmin(c)) throw new ManagementAccessDeniedException(); }
    private boolean isSchoolAdmin(InternalCallContext c) { List<String> codes = roles.findRoleCodes(c.userId()); return codes.contains("R_SUPER") || codes.contains("R_SCHOOL_ADMIN"); }
    private void requireCampusAccess(InternalCallContext c, long campus, boolean write) { if (school.countActiveCampus(c.tenantId(), campus) == 0) throw new ManagementNotFoundException(); if (isSchoolAdmin(c)) return; if (school.countCampusAdmin(c.tenantId(), campus, c.userId()) == 0) throw new ManagementAccessDeniedException(); }
    private void requireTenantUser(long userId, InternalCallContext c) { if (school.countTenantUser(c.tenantId(), userId) == 0) throw new ManagementNotFoundException(); }
    private void assignRole(long userId, String roleCode) { school.insertUserRole(userId, roleCode); }
    private static String required(Map<String, Object> values, String key, int max) { String value = text(values.get(key), max); if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required"); return value.trim(); }
    private static String text(Object value, int max) { if (value == null) return null; String result = value.toString().trim(); if (result.length() > max) throw new IllegalArgumentException("field length exceeds limit"); return result.isEmpty() ? null : result; }
    private static long number(Map<String, Object> values, String key) { Long value = optionalLong(values.get(key)); if (value == null) throw new IllegalArgumentException(key + " is required"); return value; }
    private static long number(Object value) { if (value instanceof Number number) return number.longValue(); return Long.parseLong(String.valueOf(value)); }
    private static Long optionalLong(Object value) { return value == null || value.toString().isBlank() ? null : Long.valueOf(value.toString()); }
    private static void validateGrade(long grade) { if (grade < 1 || grade > 6) throw new IllegalArgumentException("grade must be between 1 and 6"); }
    private static Long lookup(Long value) { if (value == null) throw new ManagementNotFoundException(); return value; }
    private static LocalDate dateOrToday(Object value) { return value == null ? LocalDate.now() : LocalDate.parse(value.toString()); }
    private static LocalDate toLocalDate(Object value) { if (value instanceof LocalDate date) return date; if (value instanceof java.sql.Date date) return date.toLocalDate(); return LocalDate.parse(String.valueOf(value)); }
}
