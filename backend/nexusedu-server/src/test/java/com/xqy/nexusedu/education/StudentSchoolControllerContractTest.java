package com.xqy.nexusedu.education;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.xqy.nexusedu.education.assessment.controller.AssessmentController;
import com.xqy.nexusedu.education.enrollment.controller.EnrollmentController;
import com.xqy.nexusedu.education.organization.controller.OrganizationController;
import com.xqy.nexusedu.education.student.profile.controller.StudentProfileController;
import com.xqy.nexusedu.education.teacher.controller.TeacherController;

class StudentSchoolControllerContractTest {
    @Test
    void keepsSchoolApiRoutesStable() {
        Set<String> routes = new LinkedHashSet<>();
        for (Class<?> controller : List.of(OrganizationController.class, TeacherController.class,
                EnrollmentController.class, AssessmentController.class, StudentProfileController.class)) {
            String basePath = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class).value()[0];
            for (Method method : controller.getDeclaredMethods()) {
                add(routes, basePath, "GET", method, GetMapping.class);
                add(routes, basePath, "POST", method, PostMapping.class);
                add(routes, basePath, "PUT", method, PutMapping.class);
                add(routes, basePath, "DELETE", method, DeleteMapping.class);
            }
        }

        assertEquals(Set.of(
                "GET /api/school/campuses", "POST /api/school/campuses",
                "PUT /api/school/campuses/{campusId}", "DELETE /api/school/campuses/{campusId}",
                "GET /api/school/academic-years", "POST /api/school/academic-years",
                "GET /api/school/classes", "POST /api/school/classes",
                "GET /api/school/classes/{classId}", "PUT /api/school/classes/{classId}",
                "PUT /api/school/classes/{classId}/status", "POST /api/school/classes/{classId}/students/import",
                "PUT /api/school/classes/{classId}/homeroom", "PUT /api/school/campuses/{campusId}/admins",
                "PUT /api/school/classes/{classId}/teachers", "DELETE /api/school/classes/{classId}/teachers/{assignmentId}",
                "GET /api/school/teachers", "GET /api/school/teacher-candidates", "POST /api/school/teachers",
                "GET /api/school/subjects", "POST /api/school/subjects",
                "POST /api/school/promotions/preview", "POST /api/school/promotions/confirm",
                "GET /api/school/promotions/{batchId}", "GET /api/school/exams", "POST /api/school/exams",
                "GET /api/school/scores", "POST /api/school/scores", "GET /api/school/students",
                "POST /api/school/students", "POST /api/school/students/import",
                "POST /api/school/students/{studentId}/move", "PUT /api/school/students/{studentId}/status"
        ), routes);
    }

    private static <A extends java.lang.annotation.Annotation> void add(Set<String> routes, String basePath, String verb,
                                                                         Method method, Class<A> mappingType) {
        A mapping = AnnotatedElementUtils.findMergedAnnotation(method, mappingType);
        if (mapping == null) return;
        String[] paths;
        if (mapping instanceof GetMapping get) paths = get.value().length == 0 ? get.path() : get.value();
        else if (mapping instanceof PostMapping post) paths = post.value().length == 0 ? post.path() : post.value();
        else if (mapping instanceof PutMapping put) paths = put.value().length == 0 ? put.path() : put.value();
        else if (mapping instanceof DeleteMapping delete) paths = delete.value().length == 0 ? delete.path() : delete.value();
        else throw new IllegalStateException("Unsupported mapping annotation");
        if (paths.length == 0) paths = new String[]{""};
        for (String path : paths) routes.add(verb + " " + basePath + path);
    }
}
