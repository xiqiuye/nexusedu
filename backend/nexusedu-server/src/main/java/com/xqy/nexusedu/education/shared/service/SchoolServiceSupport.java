package com.xqy.nexusedu.education.shared.service;

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
import com.xqy.nexusedu.education.infrastructure.persistence.mapper.StudentSchoolMapper;
import com.xqy.nexusedu.system.authentication.mapper.SysAuthorizationMapper;

/**
 * School-scoped student, campus and classroom use cases.
 */
public abstract class SchoolServiceSupport {
    private final StudentSchoolMapper school;
    private final SysAuthorizationMapper roles;
    private final AuthorizationPolicy authorization;
    private final TransactionTemplate transactions;

    protected SchoolServiceSupport(StudentSchoolMapper school, SysAuthorizationMapper roles,
                                   AuthorizationPolicy authorization, PlatformTransactionManager transactionManager) {
        this.school = school;
        this.roles = roles;
        this.authorization = authorization;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public List<Map<String, Object>> campuses(InternalCallContext caller) {
        requireSchoolUser(caller);
        if (isSchoolAdmin(caller)) return school.findCampuses(caller.tenantId());
        if (roles.findRoleCodes(caller.userId()).contains("R_CAMPUS_ADMIN"))
            return school.findCampusesForAdmin(caller.tenantId(), caller.userId());
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

    public Map<String, Object> classDetail(long classId, InternalCallContext caller) {
        requireSchoolUser(caller);
        Map<String, Object> result = school.findClassById(caller.tenantId(), classId);
        if (result == null) throw new ManagementNotFoundException();
        Long campus = number(result.get("campusId"));
        requireCampusReadAccess(caller, campus);
        Map<String, String> filter = Map.of("classId", String.valueOf(classId), "academicYearId", String.valueOf(result.get("academicYearId")));
        result.put("roster", students(filter, 1, 200, caller));
        result.put("teacherAssignments", school.findClassTeacherAssignments(caller.tenantId(), classId));
        return result;
    }

    public Map<String, Object> updateClass(long classId, Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        Map<String, Object> current = requireClass(classId, caller, true);
        String name = required(input, "className", 64);
        Object capacity = input.get("capacity");
        if (capacity != null && number(capacity) < 1) throw new IllegalArgumentException("capacity must be positive");
        school.updateClass(caller.tenantId(), classId, name, capacity);
        return school.findClassById(caller.tenantId(), classId);
    }

    public void updateClassStatus(long classId, String status, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        requireClass(classId, caller, true);
        if (!List.of("ACTIVE", "INACTIVE", "ARCHIVED").contains(status))
            throw new IllegalArgumentException("unsupported class status");
        if ("ARCHIVED".equals(status) && school.countEnrolledInClass(caller.tenantId(), classId) > 0)
            throw new ManagementConflictException("class still has enrolled students");
        school.updateClassStatus(caller.tenantId(), classId, status);
    }

    public List<Map<String, Object>> teachers(String search, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        return school.findTeachers(caller.tenantId(), search == null ? "" : search.trim());
    }

    public List<Map<String, Object>> teacherCandidates(String search, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        return school.findTeacherCandidates(caller.tenantId(), search == null ? "" : search.trim());
    }

    @Transactional
    public Map<String, Object> createTeacher(Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        long userId = number(input, "userId");
        requireTenantUser(userId, caller);
        String employeeNo = required(input, "employeeNo", 64);
        if (school.findTeacher(caller.tenantId(), userId) != null)
            throw new ManagementConflictException("teacher profile already exists");
        school.insertTeacher(caller.tenantId(), userId, employeeNo, text(input.get("title"), 64));
        assignRole(userId, "R_TEACHER");
        return school.findTeacher(caller.tenantId(), userId);
    }

    public List<Map<String, Object>> subjects(Long grade, InternalCallContext caller) {
        requireSchoolUser(caller);
        return school.findSubjects(caller.tenantId(), grade);
    }

    public Map<String, Object> createSubject(Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        String code = required(input, "subjectCode", 32), name = required(input, "name", 64);
        int gradeMin = Math.toIntExact(input.get("gradeMin") == null ? 1 : number(input.get("gradeMin")));
        int gradeMax = Math.toIntExact(input.get("gradeMax") == null ? 6 : number(input.get("gradeMax")));
        if (gradeMin < 1 || gradeMax > 6 || gradeMax < gradeMin)
            throw new IllegalArgumentException("invalid grade range");
        school.insertSubject(caller.tenantId(), code, name, gradeMin, gradeMax);
        return school.findSubjects(caller.tenantId(), null).stream().filter(x -> code.equals(x.get("subjectCode"))).findFirst().orElseThrow();
    }

    @Transactional
    public void assignClassTeacher(long classId, Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        Map<String, Object> clazz = requireClass(classId, caller, true);
        long userId = number(input, "userId");
        requireTenantUser(userId, caller);
        Map<String, Object> teacher = school.findTeacher(caller.tenantId(), userId);
        if (teacher == null) throw new ManagementNotFoundException();
        String type = String.valueOf(input.getOrDefault("type", "SUBJECT"));
        if (!List.of("HOMEROOM", "SUBJECT").contains(type))
            throw new IllegalArgumentException("unsupported assignment type");
        Long subjectId = optionalLong(input.get("subjectId"));
        if ("SUBJECT".equals(type) && (subjectId == null || school.countSubject(caller.tenantId(), subjectId, Math.toIntExact(number(clazz.get("grade")))) == 0))
            throw new IllegalArgumentException("subject does not match class grade");
        LocalDate start = input.get("startsOn") == null ? LocalDate.now() : LocalDate.parse(input.get("startsOn").toString());
        Long teacherId = number(teacher.get("teacherId"));
        if ("HOMEROOM".equals(type)) {
            List<Map<String, Object>> previous = school.findClassTeacherAssignments(caller.tenantId(), classId).stream()
                    .filter(row -> "HOMEROOM".equals(row.get("type")) && row.get("endsOn") == null).toList();
            school.closeClassTeachers(caller.tenantId(), classId, null, null, type, start.minusDays(1));
            school.closeHomeroom(caller.tenantId(), classId, start.minusDays(1));
            for (Map<String, Object> old : previous) {
                Long oldTeacher = number(old.get("teacherId"));
                if (!oldTeacher.equals(teacherId) && school.countOtherActiveClassTeacherAssignments(caller.tenantId(), classId, oldTeacher) == 0)
                    school.deleteClassScope(caller.tenantId(), classId, number(old.get("userId")));
            }
        } else {
            school.closeClassTeachers(caller.tenantId(), classId, teacherId, subjectId, type, start.minusDays(1));
        }
        school.insertClassTeacher(caller.tenantId(), classId, teacherId, subjectId, type, start);
        if ("HOMEROOM".equals(type)) assignHomeroom(classId, userId, start, caller);
        else school.insertClassScope(caller.tenantId(), classId, userId);
    }

    @Transactional
    public void removeClassTeacher(long classId, long assignmentId, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        requireClass(classId, caller, true);
        Map<String, Object> assignment = school.findClassTeacherAssignment(caller.tenantId(), classId, assignmentId);
        if (assignment == null) throw new ManagementNotFoundException();
        LocalDate effective = LocalDate.now();
        school.closeClassTeacherAssignment(caller.tenantId(), classId, assignmentId, effective);
        Long teacherId = number(assignment.get("teacherId")), userId = number(assignment.get("userId"));
        if ("HOMEROOM".equals(assignment.get("type")) && userId != null)
            school.closeHomeroom(caller.tenantId(), classId, effective);
        if (userId != null && school.countOtherActiveClassTeacherAssignments(caller.tenantId(), classId, teacherId) == 0)
            school.deleteClassScope(caller.tenantId(), classId, userId);
    }

    public Map<String, Object> importClassRoster(long classId, List<Map<String, Object>> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        if (input == null) throw new IllegalArgumentException("roster data cannot be null");
        Map<String, Object> clazz = requireClass(classId, caller, true);
        int imported = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            Map<String, Object> row = input.get(i);
            try {
                Map<String, Object> values = new HashMap<>(row);
                values.put("campusId", clazz.get("campusId"));
                values.put("academicYearId", clazz.get("academicYearId"));
                values.put("grade", clazz.get("grade"));
                values.put("classId", classId);
                transactions.execute(status -> {
                    createStudent(values, caller);
                    return null;
                });
                imported++;
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("row", i + 2);
                error.put("studentNo", String.valueOf(row.getOrDefault("studentNo", "")));
                error.put("message", e.getMessage() == null ? "import failed" : e.getMessage());
                errors.add(error);
            }
        }
        return Map.of("imported", imported, "total", input.size(), "errors", errors);
    }

    public Map<String, Object> previewPromotion(Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        long campus = number(input, "campusId"), sourceYear = number(input, "sourceAcademicYearId"), targetYear = number(input, "targetAcademicYearId");
        int grade = Math.toIntExact(number(input, "grade"));
        validateGrade(grade);
        if (sourceYear == targetYear || school.countAcademicYear(caller.tenantId(), targetYear) == 0 || school.countAcademicYear(caller.tenantId(), sourceYear) == 0)
            throw new IllegalArgumentException("source and target academic years must be different valid years");
        requireCampusAccess(caller, campus, true);
        List<Map<String, Object>> students = school.findPromotionStudents(caller.tenantId(), campus, sourceYear, grade);
        List<Map<String, Object>> targets = school.findClasses(caller.tenantId(), targetYear, campus);
        targets.removeIf(x -> number(x.get("grade")) != (grade == 6 ? 6 : grade + 1) && number(x.get("grade")) != grade);
        Map<String, Object> result = new HashMap<>();
        result.put("students", students);
        result.put("targetClasses", targets);
        result.put("defaultTargetGrade", grade == 6 ? null : grade + 1);
        result.put("sourceGrade", grade);
        result.put("targetAcademicYearId", targetYear);
        return result;
    }

    @Transactional
    public Map<String, Object> confirmPromotion(Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        long campus = number(input, "campusId"), sourceYear = number(input, "sourceAcademicYearId"), targetYear = number(input, "targetAcademicYearId");
        int grade = Math.toIntExact(number(input, "grade"));
        validateGrade(grade);
        requireCampusAccess(caller, campus, true);
        if (sourceYear == targetYear || school.countAcademicYear(caller.tenantId(), sourceYear) == 0 || school.countAcademicYear(caller.tenantId(), targetYear) == 0)
            throw new IllegalArgumentException("source and target academic years must be different valid years");
        List<Map<String, Object>> decisions = castRows(input.get("students"));
        if (decisions.isEmpty()) throw new IllegalArgumentException("promotion decisions are required");
        List<Map<String, Object>> source = school.findPromotionStudents(caller.tenantId(), campus, sourceYear, grade);
        if (decisions.size() != source.size())
            throw new IllegalArgumentException("promotion decisions must include every source student");
        Map<Long, Map<String, Object>> sourceByStudent = new HashMap<>();
        for (Map<String, Object> row : source) sourceByStudent.put(number(row.get("studentId")), row);
        java.util.Set<Long> decidedStudents = new java.util.HashSet<>();
        for (Map<String, Object> decision : decisions) {
            Long studentId = number(decision.get("studentId"));
            if (!sourceByStudent.containsKey(studentId) || !decidedStudents.add(studentId))
                throw new IllegalArgumentException("promotion list contains an invalid or duplicate student");
        }
        school.insertPromotionBatch(caller.tenantId(), campus, sourceYear, targetYear, grade, caller.userId());
        Long batchId = school.lastInsertId();
        LocalDate effective = dateOrToday(input.get("effectiveOn"));
        int changed = 0;
        for (Map<String, Object> decision : decisions) {
            Long studentId = number(decision.get("studentId"));
            Map<String, Object> old = sourceByStudent.get(studentId);
            if (school.countTargetEnrollment(caller.tenantId(), studentId, targetYear) > 0)
                throw new ManagementConflictException("student already has target-year enrollment");
            String action = String.valueOf(decision.getOrDefault("action", "PROMOTE"));
            if (!List.of("PROMOTE", "RETAIN", "GRADUATE").contains(action))
                throw new IllegalArgumentException("unsupported promotion action");
            Integer targetGrade = "GRADUATE".equals(action) ? null : Math.toIntExact(number(decision.getOrDefault("targetGrade", "PROMOTE".equals(action) ? grade + 1 : grade)));
            if ("PROMOTE".equals(action) && (grade == 6 || targetGrade != grade + 1))
                throw new IllegalArgumentException("promotion must advance exactly one grade");
            if ("RETAIN".equals(action) && targetGrade != grade)
                throw new IllegalArgumentException("retained students must remain in the same grade");
            if ("GRADUATE".equals(action) && grade != 6)
                throw new IllegalArgumentException("only grade six students may graduate");
            if (targetGrade != null) validateGrade(targetGrade);
            Long classId = optionalLong(decision.get("targetClassId"));
            if (targetGrade != null && (classId == null || school.countTargetClass(caller.tenantId(), classId, campus, targetYear, targetGrade.longValue()) == 0))
                throw new IllegalArgumentException("target class does not match promotion decision");
            school.updateEnrollmentEnd(caller.tenantId(), number(old.get("enrollmentId")), effective);
            String status = "GRADUATE".equals(action) ? "GRADUATED" : "ENROLLED";
            school.insertPromotionEnrollment(caller.tenantId(), studentId, targetYear, campus, targetGrade, classId, status, effective, caller.userId());
            Long targetEnrollment = school.findEnrollmentIdForYear(caller.tenantId(), studentId, targetYear);
            school.insertPromotionItem(caller.tenantId(), batchId, studentId, number(old.get("enrollmentId")), targetEnrollment, action, targetGrade, classId, text(decision.get("reason"), 255));
            changed++;
        }
        return Map.of("batchId", batchId, "processed", changed, "status", "CONFIRMED");
    }

    public Map<String, Object> promotionBatch(long batchId, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        Map<String, Object> batch = school.findPromotionBatch(caller.tenantId(), batchId);
        if (batch == null) throw new ManagementNotFoundException();
        batch.put("items", school.findPromotionItems(caller.tenantId(), batchId));
        return batch;
    }

    public List<Map<String, Object>> exams(Map<String, String> filters, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        filters = filters == null ? Map.of() : filters;
        return school.findExams(caller.tenantId(), optionalLong(filters.get("academicYearId")), optionalLong(filters.get("campusId")), optionalLong(filters.get("grade")));
    }

    public Map<String, Object> createExam(Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        long year = number(input, "academicYearId"), campus = number(input, "campusId");
        int grade = Math.toIntExact(number(input, "grade"));
        validateGrade(grade);
        requireCampusAccess(caller, campus, true);
        if (school.countAcademicYear(caller.tenantId(), year) == 0) throw new ManagementNotFoundException();
        String term = required(input, "term", 16);
        if (!List.of("FIRST", "SECOND", "SUMMER").contains(term))
            throw new IllegalArgumentException("unsupported term");
        LocalDate date = LocalDate.parse(required(input, "examDate", 10));
        school.insertExam(caller.tenantId(), year, campus, grade, required(input, "name", 128), term, date, caller.userId());
        return school.findExam(caller.tenantId(), school.lastInsertId());
    }

    public List<Map<String, Object>> scores(long examId, Long classId, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        Map<String, Object> exam = school.findExam(caller.tenantId(), examId);
        if (exam == null) throw new ManagementNotFoundException();
        if (classId != null) requireClass(classId, caller, true);
        return school.findScores(caller.tenantId(), examId, classId);
    }

    @Transactional
    public void saveScore(Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        long examId = number(input, "examId"), studentId = number(input, "studentId"), subjectId = number(input, "subjectId");
        Map<String, Object> exam = school.findExam(caller.tenantId(), examId);
        if (exam == null) throw new ManagementNotFoundException();
        int grade = Math.toIntExact(number(exam.get("grade")));
        if (school.countSubject(caller.tenantId(), subjectId, grade) == 0)
            throw new IllegalArgumentException("subject does not match exam grade");
        Map<String, Object> enrollment = school.findActiveEnrollment(caller.tenantId(), studentId, number(exam.get("academicYearId")), optionalLong(input.get("classId")));
        if (enrollment == null || number(enrollment.get("campusId")) != number(exam.get("campusId")) || number(enrollment.get("grade")) != grade)
            throw new IllegalArgumentException("student is not enrolled in the exam scope");
        boolean absent = Boolean.parseBoolean(String.valueOf(input.getOrDefault("absent", false)));
        Object score = absent ? null : input.get("score");
        if (!absent && score == null) throw new IllegalArgumentException("score is required unless student is absent");
        if (score != null && (Double.parseDouble(score.toString()) < 0 || Double.parseDouble(score.toString()) > 100))
            throw new IllegalArgumentException("score must be between 0 and 100");
        school.upsertScore(caller.tenantId(), examId, studentId, number(enrollment.get("enrollmentId")), number(enrollment.get("classId")), subjectId, score, absent, text(input.get("remark"), 255), caller.userId());
    }

    public PageResult students(Map<String, String> filter, int current, int size, InternalCallContext caller) {
        requireSchoolUser(caller);
        filter = filter == null ? Map.of() : filter;
        current = Math.max(1, current);
        size = Math.max(1, Math.min(200, size));
        Long campus = optionalLong(filter.get("campusId")), year = optionalLong(filter.get("academicYearId")), grade = optionalLong(filter.get("grade")), classId = optionalLong(filter.get("classId"));
        String search = filter.getOrDefault("search", "").trim(), status = filter.getOrDefault("status", "").trim();
        String sortColumn = switch (filter.getOrDefault("sortBy", "studentNo")) {
            case "name" -> "s.name";
            case "grade" -> "e.grade";
            case "className" -> "c.class_name";
            case "campusName" -> "p.name";
            case "enrolledOn" -> "e.starts_on";
            default -> "s.student_no";
        };
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
        requireCampusAccess(caller, campus, true);
        validateGrade(grade);
        String no = required(input, "studentNo", 64), name = required(input, "name", 128);
        Long classId = optionalLong(input.get("classId"));
        if (classId != null && school.countActiveClass(caller.tenantId(), classId, campus, year, grade) == 0)
            throw new IllegalArgumentException("class does not match campus, academic year or grade");
        String email = text(input.get("email"), 128), phone = text(input.get("phone"), 32);
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("invalid email format");
        if (phone != null && !phone.matches("^[0-9+() -]{6,32}$"))
            throw new IllegalArgumentException("invalid phone format");
        if (school.countStudentNo(caller.tenantId(), no) > 0)
            throw new ManagementConflictException("student number already exists");
        school.insertStudent(caller.tenantId(), no, name, text(input.get("gender"), 16), input.get("birthday"), email, phone);
        Long id = school.findStudentId(caller.tenantId(), no);
        school.insertEnrollment(caller.tenantId(), id, year, campus, grade, classId, "ENROLLED", LocalDate.now(), null, null, caller.userId());
        return school.findStudent(caller.tenantId(), id);
    }

    public Map<String, Object> importStudents(List<Map<String, Object>> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        if (input == null) throw new IllegalArgumentException("import data cannot be null");
        int imported = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            Map<String, Object> row = input.get(i);
            try {
                transactions.execute(status -> {
                    Map<String, Object> values = new HashMap<>(row);
                    if (values.get("campusId") == null)
                        values.put("campusId", lookup(school.findCampusIdByName(caller.tenantId(), required(values, "campusName", 128))));
                    if (values.get("academicYearId") == null)
                        values.put("academicYearId", lookup(school.findAcademicYearIdByName(caller.tenantId(), required(values, "academicYear", 32))));
                    if (values.get("grade") == null) values.put("grade", values.get("gradeNo"));
                    if (values.get("classId") == null && values.get("className") != null && !values.get("className").toString().isBlank())
                        values.put("classId", lookup(school.findClassId(caller.tenantId(), optionalLong(values.get("campusId")), optionalLong(values.get("academicYearId")), optionalLong(values.get("grade")), values.get("className").toString())));
                    createStudent(values, caller);
                    return null;
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
        requireSchoolAdmin(caller);
        Long enrollment = optionalLong(input.get("enrollmentId"));
        if (enrollment == null) throw new IllegalArgumentException("enrollment record is required");
        Map<String, Object> old = school.findEnrollmentForMove(caller.tenantId(), studentId, enrollment);
        if (old == null) throw new ManagementNotFoundException();
        requireCampusAccess(caller, number(old.get("campusId")), true);
        long campus = number(input, "campusId"), grade = number(input, "grade");
        Long classId = optionalLong(input.get("classId"));
        requireCampusAccess(caller, campus, true);
        validateGrade(grade);
        if (classId != null && school.countTargetClass(caller.tenantId(), classId, campus, number(old.get("yearId")), grade) == 0)
            throw new IllegalArgumentException("target class does not match");
        String reason = text(input.get("reason"), 255);
        LocalDate effective = dateOrToday(input.get("effectiveOn"));
        school.updateEnrollmentTransfer(caller.tenantId(), enrollment, effective, reason, caller.userId());
        school.insertEnrollmentTransfer(caller.tenantId(), studentId, number(old.get("yearId")), campus, grade, classId, effective, reason, caller.userId());
    }

    @Transactional
    public void changeEnrollmentStatus(long studentId, Map<String, Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        Long enrollment = optionalLong(input.get("enrollmentId"));
        String status = required(input, "status", 20);
        if (!List.of("SUSPENDED", "ENROLLED", "TRANSFERRED", "GRADUATED").contains(status))
            throw new IllegalArgumentException("unsupported enrollment status");
        Map<String, Object> old = school.findEnrollmentForStatus(caller.tenantId(), studentId, enrollment);
        if (old == null) throw new ManagementNotFoundException();
        requireCampusAccess(caller, number(old.get("campusId")), true);
        LocalDate effective = dateOrToday(input.get("effectiveOn"));
        if (effective.isBefore(toLocalDate(old.get("startsOn"))))
            throw new IllegalArgumentException("effective date cannot be before enrollment start");
        if (status.equals(old.get("status"))) throw new IllegalArgumentException("enrollment status is unchanged");
        String reason = text(input.get("reason"), 255);
        school.updateEnrollmentStatus(caller.tenantId(), enrollment, effective, reason, caller.userId());
        school.insertEnrollmentStatus(caller.tenantId(), studentId, number(old.get("yearId")), number(old.get("campusId")), number(old.get("grade")), optionalLong(old.get("classId")), status, effective, reason, caller.userId());
    }

    public void assignHomeroom(long classId, long userId, LocalDate start, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        Long campus = school.findClassCampusId(caller.tenantId(), classId);
        if (campus == null) throw new ManagementNotFoundException();
        requireCampusAccess(caller, campus, true);
        requireTenantUser(userId, caller);
        assignRole(userId, "R_HOMEROOM");
        school.closeHomeroom(caller.tenantId(), classId, start.minusDays(1));
        school.insertHomeroom(caller.tenantId(), classId, userId, start);
    }

    public void assignCampusAdmin(long campusId, long userId, InternalCallContext caller) {
        requireSuper(caller);
        requireCampusAccess(caller, campusId, true);
        requireTenantUser(userId, caller);
        assignRole(userId, "R_CAMPUS_ADMIN");
        school.insertCampusAdmin(caller.tenantId(), campusId, userId);
    }

    public void assignTeacher(long classId, long userId, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        Long campus = school.findClassCampusId(caller.tenantId(), classId);
        if (campus == null) throw new ManagementNotFoundException();
        requireCampusAccess(caller, campus, true);
        requireTenantUser(userId, caller);
        assignRole(userId, "R_TEACHER");
        school.insertClassScope(caller.tenantId(), classId, userId);
    }

    private Map<String, Object> requireClass(long classId, InternalCallContext caller, boolean write) {
        Map<String, Object> clazz = school.findClassById(caller.tenantId(), classId);
        if (clazz == null) throw new ManagementNotFoundException();
        if (write) {
            requireCampusAccess(caller, number(clazz.get("campusId")), true);
        } else requireCampusReadAccess(caller, number(clazz.get("campusId")), classId);
        return clazz;
    }

    private void requireCampusReadAccess(InternalCallContext caller, long campus) {
        requireCampusReadAccess(caller, campus, null);
    }

    private void requireCampusReadAccess(InternalCallContext caller, long campus, Long classId) {
        if (isSchoolAdmin(caller)) return;
        if (school.countCampusAdmin(caller.tenantId(), campus, caller.userId()) > 0) return;
        if (school.findClassesForUser(caller.tenantId(), null, campus, caller.userId()).stream().anyMatch(x -> classId == null || number(x.get("id")) == classId))
            return;
        throw new ManagementAccessDeniedException();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castRows(Object value) {
        if (!(value instanceof List<?> rows)) throw new IllegalArgumentException("students must be an array");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map))
                throw new IllegalArgumentException("student decision must be an object");
            result.add((Map<String, Object>) map);
        }
        return result;
    }

    private void requireSchoolUser(InternalCallContext c) {
        authorization.authorize("student:read", c);
        if (roles.findRoleCodes(c.userId()).stream().noneMatch(r -> List.of("R_SUPER", "R_SCHOOL_ADMIN", "R_CAMPUS_ADMIN", "R_HOMEROOM", "R_TEACHER").contains(r)))
            throw new ManagementAccessDeniedException();
    }

    private void requireSchoolAdmin(InternalCallContext c) {
        authorization.authorize("student:write", c);
        if (roles.findRoleCodes(c.userId()).stream().noneMatch(r -> List.of("R_SUPER", "R_SCHOOL_ADMIN", "R_CAMPUS_ADMIN").contains(r)))
            throw new ManagementAccessDeniedException();
    }

    protected final void requireSuper(InternalCallContext c) {
        authorization.authorize("student:write", c);
        if (!isSchoolAdmin(c)) throw new ManagementAccessDeniedException();
    }

    private boolean isSchoolAdmin(InternalCallContext c) {
        List<String> codes = roles.findRoleCodes(c.userId());
        return codes.contains("R_SUPER") || codes.contains("R_SCHOOL_ADMIN");
    }

    private void requireCampusAccess(InternalCallContext c, long campus, boolean write) {
        if (school.countActiveCampus(c.tenantId(), campus) == 0) throw new ManagementNotFoundException();
        if (isSchoolAdmin(c)) return;
        if (school.countCampusAdmin(c.tenantId(), campus, c.userId()) == 0) throw new ManagementAccessDeniedException();
    }

    private void requireTenantUser(long userId, InternalCallContext c) {
        if (school.countTenantUser(c.tenantId(), userId) == 0) throw new ManagementNotFoundException();
    }

    private void assignRole(long userId, String roleCode) {
        school.insertUserRole(userId, roleCode);
    }

    protected static String required(Map<String, Object> values, String key, int max) {
        String value = text(values.get(key), max);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is required");
        return value.trim();
    }

    protected static String text(Object value, int max) {
        if (value == null) return null;
        String result = value.toString().trim();
        if (result.length() > max) throw new IllegalArgumentException("field length exceeds limit");
        return result.isEmpty() ? null : result;
    }

    private static long number(Map<String, Object> values, String key) {
        Long value = optionalLong(values.get(key));
        if (value == null) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private static Long optionalLong(Object value) {
        return value == null || value.toString().isBlank() ? null : Long.valueOf(value.toString());
    }

    private static void validateGrade(long grade) {
        if (grade < 1 || grade > 6) throw new IllegalArgumentException("grade must be between 1 and 6");
    }

    private static Long lookup(Long value) {
        if (value == null) throw new ManagementNotFoundException();
        return value;
    }

    private static LocalDate dateOrToday(Object value) {
        return value == null ? LocalDate.now() : LocalDate.parse(value.toString());
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        return LocalDate.parse(String.valueOf(value));
    }
}
