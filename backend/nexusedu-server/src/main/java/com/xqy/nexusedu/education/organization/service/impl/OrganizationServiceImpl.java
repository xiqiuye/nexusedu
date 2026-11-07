package com.xqy.nexusedu.education.organization.service.impl;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.xqy.nexusedu.education.shared.service.SchoolServiceSupport;
import com.xqy.nexusedu.education.organization.service.OrganizationService;
import com.xqy.nexusedu.education.infrastructure.persistence.mapper.StudentSchoolMapper;
import com.xqy.nexusedu.system.authentication.mapper.SysAuthorizationMapper;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementConflictException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;

@Service
public class OrganizationServiceImpl extends SchoolServiceSupport implements OrganizationService {
    private final StudentSchoolMapper school;

    public OrganizationServiceImpl(StudentSchoolMapper school, SysAuthorizationMapper roles,
            AuthorizationPolicy authorization, PlatformTransactionManager transactionManager) {
        super(school, roles, authorization, transactionManager);
        this.school = school;
    }

    @Override
    public Map<String, Object> createCampus(Map<String, Object> input, InternalCallContext caller) {
        try {
            return super.createCampus(input, caller);
        } catch (DuplicateKeyException e) {
            throw new ManagementConflictException("校区编码已存在，请使用其他编码");
        }
    }

    @Override
    @Transactional
    public Map<String, Object> updateCampus(long campusId, Map<String, Object> input, InternalCallContext caller) {
        requireSuper(caller);
        if (school.findCampusForUpdate(caller.tenantId(), campusId) == null) throw new ManagementNotFoundException();
        String code = required(input, "campusCode", 32);
        String name = required(input, "name", 128);
        String address = text(input.get("address"), 255);
        try {
            school.updateCampus(caller.tenantId(), campusId, code, name, address);
        } catch (DuplicateKeyException e) {
            throw new ManagementConflictException("校区编码已存在，请使用其他编码");
        }
        return school.findCampus(caller.tenantId(), code);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteCampus(long campusId, InternalCallContext caller) {
        requireSuper(caller);
        if (school.findCampusForUpdate(caller.tenantId(), campusId) == null) throw new ManagementNotFoundException();
        if (school.countCampusReferences(caller.tenantId(), campusId) > 0) {
            throw new ManagementConflictException("校区存在班级、学籍、管理员、考试或升班记录，无法删除");
        }
        try {
            if (school.deleteCampus(caller.tenantId(), campusId) == 0) throw new ManagementNotFoundException();
        } catch (DataIntegrityViolationException e) {
            throw new ManagementConflictException("校区存在关联记录，无法删除");
        }
    }
}
