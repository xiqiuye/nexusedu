package com.xqy.nexusedu.student.profile.mapper;

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
