package com.xqy.nexusedu.education.enrollment.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import com.xqy.nexusedu.education.shared.service.SchoolServiceSupport;
import com.xqy.nexusedu.education.enrollment.service.EnrollmentService;
import com.xqy.nexusedu.education.infrastructure.persistence.mapper.StudentSchoolMapper;
import com.xqy.nexusedu.system.authentication.mapper.SysAuthorizationMapper;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;

@Service
public class EnrollmentServiceImpl extends SchoolServiceSupport implements EnrollmentService {
    public EnrollmentServiceImpl(StudentSchoolMapper school, SysAuthorizationMapper roles,
            AuthorizationPolicy authorization, PlatformTransactionManager transactionManager) {
        super(school, roles, authorization, transactionManager);
    }
}
