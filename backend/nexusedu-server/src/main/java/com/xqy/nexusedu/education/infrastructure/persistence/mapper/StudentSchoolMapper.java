package com.xqy.nexusedu.education.infrastructure.persistence.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentSchoolMapper {
    List<Map<String, Object>> findCampuses(@Param("tenantId") String tenantId);

    List<Map<String, Object>> findCampusesForAdmin(@Param("tenantId") String tenantId, @Param("userId") Long userId);

    List<Map<String, Object>> findCampusesForHomeroom(@Param("tenantId") String tenantId, @Param("userId") Long userId);

    int insertCampus(@Param("tenantId") String tenantId, @Param("campusCode") String campusCode, @Param("name") String name, @Param("address") String address);

    Map<String, Object> findCampus(@Param("tenantId") String tenantId, @Param("campusCode") String campusCode);

    Map<String, Object> findCampusForUpdate(@Param("tenantId") String tenantId, @Param("campusId") long campusId);

    int updateCampus(@Param("tenantId") String tenantId, @Param("campusId") long campusId,
                     @Param("campusCode") String campusCode, @Param("name") String name, @Param("address") String address);

    int countCampusReferences(@Param("tenantId") String tenantId, @Param("campusId") long campusId);

    int deleteCampus(@Param("tenantId") String tenantId, @Param("campusId") long campusId);

    List<Map<String, Object>> findAcademicYears(@Param("tenantId") String tenantId);

    int countCurrentAcademicYear(@Param("tenantId") String tenantId);

    int clearCurrentAcademicYears(@Param("tenantId") String tenantId);

    int insertAcademicYear(@Param("tenantId") String tenantId, @Param("name") String name, @Param("startsOn") LocalDate startsOn, @Param("endsOn") LocalDate endsOn, @Param("current") boolean current);

    Map<String, Object> findAcademicYear(@Param("tenantId") String tenantId, @Param("name") String name);

    List<Map<String, Object>> findClasses(@Param("tenantId") String tenantId, @Param("yearId") Long yearId, @Param("campusId") Long campusId);

    List<Map<String, Object>> findClassesForUser(@Param("tenantId") String tenantId, @Param("yearId") Long yearId, @Param("campusId") Long campusId, @Param("userId") Long userId);

    int countAcademicYear(@Param("tenantId") String tenantId, @Param("yearId") Long yearId);

    int insertClass(@Param("tenantId") String tenantId, @Param("campusId") Long campusId, @Param("academicYearId") Long academicYearId, @Param("grade") Long grade, @Param("className") String className, @Param("capacity") Object capacity);

    Map<String, Object> findClass(@Param("tenantId") String tenantId, @Param("campusId") Long campusId, @Param("academicYearId") Long academicYearId, @Param("grade") Long grade, @Param("className") String className);

    Map<String, Object> findClassById(@Param("tenantId") String tenantId, @Param("classId") Long classId);

    List<Map<String, Object>> findClassTeacherAssignments(@Param("tenantId") String tenantId, @Param("classId") Long classId);

    Map<String, Object> findClassTeacherAssignment(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("assignmentId") Long assignmentId);

    int closeClassTeacherAssignment(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("assignmentId") Long assignmentId, @Param("endsOn") LocalDate endsOn);

    int countOtherActiveClassTeacherAssignments(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("teacherId") Long teacherId);

    int deleteClassScope(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("userId") Long userId);

    int updateClass(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("className") String className, @Param("capacity") Object capacity);

    int updateClassStatus(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("status") String status);

    int countEnrolledInClass(@Param("tenantId") String tenantId, @Param("classId") Long classId);

    List<Map<String, Object>> findTeachers(@Param("tenantId") String tenantId, @Param("search") String search);

    List<Map<String, Object>> findTeacherCandidates(@Param("tenantId") String tenantId, @Param("search") String search);

    int insertTeacher(@Param("tenantId") String tenantId, @Param("userId") Long userId, @Param("employeeNo") String employeeNo, @Param("title") String title);

    Map<String, Object> findTeacher(@Param("tenantId") String tenantId, @Param("userId") Long userId);

    List<Map<String, Object>> findSubjects(@Param("tenantId") String tenantId, @Param("grade") Long grade);

    int insertSubject(@Param("tenantId") String tenantId, @Param("code") String code, @Param("name") String name, @Param("gradeMin") int gradeMin, @Param("gradeMax") int gradeMax);

    int countSubject(@Param("tenantId") String tenantId, @Param("subjectId") Long subjectId, @Param("grade") int grade);

    int closeClassTeachers(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("teacherId") Long teacherId, @Param("subjectId") Long subjectId, @Param("type") String type, @Param("endsOn") LocalDate endsOn);

    int insertClassTeacher(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("teacherId") Long teacherId, @Param("subjectId") Long subjectId, @Param("type") String type, @Param("startsOn") LocalDate startsOn);

    List<Map<String, Object>> findPromotionStudents(@Param("tenantId") String tenantId, @Param("campusId") Long campusId, @Param("yearId") Long yearId, @Param("grade") int grade);

    int insertPromotionBatch(@Param("tenantId") String tenantId, @Param("campusId") Long campusId, @Param("sourceYearId") Long sourceYearId, @Param("targetYearId") Long targetYearId, @Param("grade") int grade, @Param("createdBy") Long createdBy);

    Long lastInsertId();

    int updateEnrollmentEnd(@Param("tenantId") String tenantId, @Param("enrollmentId") Long enrollmentId, @Param("endsOn") LocalDate endsOn);

    int insertPromotionEnrollment(@Param("tenantId") String tenantId, @Param("studentId") Long studentId, @Param("yearId") Long yearId, @Param("campusId") Long campusId, @Param("grade") Integer grade, @Param("classId") Long classId, @Param("status") String status, @Param("startsOn") LocalDate startsOn, @Param("changedBy") Long changedBy);

    Long findEnrollmentIdForYear(@Param("tenantId") String tenantId, @Param("studentId") Long studentId, @Param("yearId") Long yearId);

    int insertPromotionItem(@Param("tenantId") String tenantId, @Param("batchId") Long batchId, @Param("studentId") Long studentId, @Param("sourceEnrollmentId") Long sourceEnrollmentId, @Param("targetEnrollmentId") Long targetEnrollmentId, @Param("action") String action, @Param("targetGrade") Integer targetGrade, @Param("targetClassId") Long targetClassId, @Param("reason") String reason);

    Map<String, Object> findPromotionBatch(@Param("tenantId") String tenantId, @Param("batchId") Long batchId);

    List<Map<String, Object>> findPromotionItems(@Param("tenantId") String tenantId, @Param("batchId") Long batchId);

    int countTargetEnrollment(@Param("tenantId") String tenantId, @Param("studentId") Long studentId, @Param("yearId") Long yearId);

    List<Map<String, Object>> findExams(@Param("tenantId") String tenantId, @Param("yearId") Long yearId, @Param("campusId") Long campusId, @Param("grade") Long grade);

    int insertExam(@Param("tenantId") String tenantId, @Param("yearId") Long yearId, @Param("campusId") Long campusId, @Param("grade") int grade, @Param("name") String name, @Param("term") String term, @Param("date") LocalDate date, @Param("createdBy") Long createdBy);

    Map<String, Object> findExam(@Param("tenantId") String tenantId, @Param("examId") Long examId);

    Map<String, Object> findActiveEnrollment(@Param("tenantId") String tenantId, @Param("studentId") Long studentId, @Param("yearId") Long yearId, @Param("classId") Long classId);

    int upsertScore(@Param("tenantId") String tenantId, @Param("examId") Long examId, @Param("studentId") Long studentId, @Param("enrollmentId") Long enrollmentId, @Param("classId") Long classId, @Param("subjectId") Long subjectId, @Param("score") Object score, @Param("absent") boolean absent, @Param("remark") String remark, @Param("modifiedBy") Long modifiedBy);

    List<Map<String, Object>> findScores(@Param("tenantId") String tenantId, @Param("examId") Long examId, @Param("classId") Long classId);

    Long countStudents(@Param("tenantId") String tenantId, @Param("campusId") Long campusId, @Param("academicYearId") Long academicYearId, @Param("grade") Long grade, @Param("classId") Long classId,
                       @Param("status") String status, @Param("search") String search, @Param("campusAdmin") boolean campusAdmin, @Param("schoolAdmin") boolean schoolAdmin, @Param("userId") Long userId,
                       @Param("useCurrentYear") boolean useCurrentYear);

    List<Map<String, Object>> findStudents(@Param("tenantId") String tenantId, @Param("campusId") Long campusId, @Param("academicYearId") Long academicYearId, @Param("grade") Long grade,
                                           @Param("classId") Long classId, @Param("status") String status, @Param("search") String search, @Param("campusAdmin") boolean campusAdmin,
                                           @Param("schoolAdmin") boolean schoolAdmin, @Param("userId") Long userId, @Param("useCurrentYear") boolean useCurrentYear,
                                           @Param("sortColumn") String sortColumn, @Param("sortDirection") String sortDirection, @Param("limit") int limit, @Param("offset") long offset);

    int countActiveClass(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("campusId") Long campusId, @Param("academicYearId") Long academicYearId, @Param("grade") Long grade);

    int countStudentNo(@Param("tenantId") String tenantId, @Param("studentNo") String studentNo);

    int insertStudent(@Param("tenantId") String tenantId, @Param("studentNo") String studentNo, @Param("name") String name, @Param("gender") String gender, @Param("birthday") Object birthday,
                      @Param("email") String email, @Param("phone") String phone);

    Long findStudentId(@Param("tenantId") String tenantId, @Param("studentNo") String studentNo);

    Map<String, Object> findStudent(@Param("tenantId") String tenantId, @Param("studentId") Long studentId);

    int insertEnrollment(@Param("tenantId") String tenantId, @Param("studentId") Long studentId, @Param("academicYearId") Long academicYearId, @Param("campusId") Long campusId, @Param("grade") Long grade,
                         @Param("classId") Long classId, @Param("status") String status, @Param("startsOn") LocalDate startsOn, @Param("endsOn") LocalDate endsOn, @Param("reason") String reason, @Param("changedBy") Long changedBy);

    Long findCampusIdByName(@Param("tenantId") String tenantId, @Param("name") String name);

    Long findAcademicYearIdByName(@Param("tenantId") String tenantId, @Param("name") String name);

    Long findClassId(@Param("tenantId") String tenantId, @Param("campusId") Long campusId, @Param("academicYearId") Long academicYearId, @Param("grade") Long grade, @Param("className") String className);

    Map<String, Object> findEnrollmentForMove(@Param("tenantId") String tenantId, @Param("studentId") Long studentId, @Param("enrollmentId") Long enrollmentId);

    int countTargetClass(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("campusId") Long campusId, @Param("academicYearId") Long academicYearId, @Param("grade") Long grade);

    int updateEnrollmentTransfer(@Param("tenantId") String tenantId, @Param("enrollmentId") Long enrollmentId, @Param("endsOn") LocalDate endsOn, @Param("reason") String reason, @Param("changedBy") Long changedBy);

    int insertEnrollmentTransfer(@Param("tenantId") String tenantId, @Param("studentId") Long studentId, @Param("academicYearId") Long academicYearId, @Param("campusId") Long campusId, @Param("grade") Long grade,
                                 @Param("classId") Long classId, @Param("startsOn") LocalDate startsOn, @Param("reason") String reason, @Param("changedBy") Long changedBy);

    Map<String, Object> findEnrollmentForStatus(@Param("tenantId") String tenantId, @Param("studentId") Long studentId, @Param("enrollmentId") Long enrollmentId);

    int updateEnrollmentStatus(@Param("tenantId") String tenantId, @Param("enrollmentId") Long enrollmentId, @Param("endsOn") LocalDate endsOn, @Param("reason") String reason, @Param("changedBy") Long changedBy);

    int insertEnrollmentStatus(@Param("tenantId") String tenantId, @Param("studentId") Long studentId, @Param("academicYearId") Long academicYearId, @Param("campusId") Long campusId, @Param("grade") Long grade,
                               @Param("classId") Long classId, @Param("status") String status, @Param("startsOn") LocalDate startsOn, @Param("reason") String reason, @Param("changedBy") Long changedBy);

    Long findClassCampusId(@Param("tenantId") String tenantId, @Param("classId") Long classId);

    int closeHomeroom(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("endsOn") LocalDate endsOn);

    int insertHomeroom(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("userId") Long userId, @Param("startsOn") LocalDate startsOn);

    int insertCampusAdmin(@Param("tenantId") String tenantId, @Param("campusId") Long campusId, @Param("userId") Long userId);

    int insertClassScope(@Param("tenantId") String tenantId, @Param("classId") Long classId, @Param("userId") Long userId);

    int countActiveCampus(@Param("tenantId") String tenantId, @Param("campusId") Long campusId);

    int countCampusAdmin(@Param("tenantId") String tenantId, @Param("campusId") Long campusId, @Param("userId") Long userId);

    int countTenantUser(@Param("tenantId") String tenantId, @Param("userId") Long userId);

    int insertUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
