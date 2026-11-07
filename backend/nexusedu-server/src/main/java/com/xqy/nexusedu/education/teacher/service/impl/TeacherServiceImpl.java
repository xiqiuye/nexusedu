package com.xqy.nexusedu.education.teacher.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import com.xqy.nexusedu.education.shared.service.SchoolServiceSupport;
import com.xqy.nexusedu.education.teacher.service.TeacherService;
import com.xqy.nexusedu.education.infrastructure.persistence.mapper.StudentSchoolMapper;
import com.xqy.nexusedu.system.authentication.mapper.SysAuthorizationMapper;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;

@Service
public class TeacherServiceImpl extends SchoolServiceSupport implements TeacherService {
    public TeacherServiceImpl(StudentSchoolMapper school, SysAuthorizationMapper roles,
            AuthorizationPolicy authorization, PlatformTransactionManager transactionManager) {
        super(school, roles, authorization, transactionManager);
    }
}
