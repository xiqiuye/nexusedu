package com.xqy.nexusedu.education.application.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.xqy.nexusedu.education.application.controller.StudentSchoolController;

class StudentSchoolControllerContractTest {
    @Test
    void keepsSchoolApiRoutesStable() {
        Set<String> routes = new LinkedHashSet<>();
        for (Method method : StudentSchoolController.class.getDeclaredMethods()) {
            add(routes, "GET", method, GetMapping.class);
            add(routes, "POST", method, PostMapping.class);
            add(routes, "PUT", method, PutMapping.class);
            add(routes, "DELETE", method, DeleteMapping.class);
        }

        assertEquals(Set.of(
                "GET /api/school/campuses", "POST /api/school/campuses",
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

    private static <A extends java.lang.annotation.Annotation> void add(Set<String> routes, String verb,
                                                                         Method method, Class<A> mappingType) {
        A mapping = AnnotatedElementUtils.findMergedAnnotation(method, mappingType);
        if (mapping == null) return;
        String[] paths;
        if (mapping instanceof GetMapping get) paths = get.value().length == 0 ? get.path() : get.value();
        else if (mapping instanceof PostMapping post) paths = post.value().length == 0 ? post.path() : post.value();
        else if (mapping instanceof PutMapping put) paths = put.value().length == 0 ? put.path() : put.value();
        else if (mapping instanceof DeleteMapping delete) paths = delete.value().length == 0 ? delete.path() : delete.value();
        else throw new IllegalStateException("Unsupported mapping annotation");
        String basePath = AnnotatedElementUtils.findMergedAnnotation(StudentSchoolController.class, RequestMapping.class).value()[0];
        for (String path : paths) routes.add(verb + " " + basePath + path);
    }
}
