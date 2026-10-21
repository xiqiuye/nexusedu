package com.xqy.nexusedu.student.profile.infrastructure.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.xqy.nexusedu.shared.kernel.application.dto.PageResult;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementAccessDeniedException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementNotFoundException;
import com.xqy.nexusedu.shared.kernel.application.exception.ManagementConflictException;
import com.xqy.nexusedu.shared.kernel.application.gateway.AuthorizationPolicy;
import com.xqy.nexusedu.shared.kernel.infrastructure.security.rpc.InternalCallContext;
import com.xqy.nexusedu.system.authentication.infrastructure.persistence.mapper.SysAuthorizationMapper;

/** School-scoped student, campus and classroom use cases. */
@Service
public class StudentSchoolService {
    private final JdbcTemplate jdbc;
    private final SysAuthorizationMapper roles;
    private final AuthorizationPolicy authorization;
    private final TransactionTemplate transactions;

    public StudentSchoolService(JdbcTemplate jdbc, SysAuthorizationMapper roles, AuthorizationPolicy authorization, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.roles = roles;
        this.authorization = authorization;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public List<Map<String,Object>> campuses(InternalCallContext caller) {
        requireSchoolUser(caller);
        if(isSchoolAdmin(caller)) return jdbc.queryForList("SELECT id,campus_code AS campusCode,name,address,status FROM school_campus WHERE tenant_id=? ORDER BY name,id", caller.tenantId());
        if(roles.findRoleCodes(caller.userId()).contains("R_CAMPUS_ADMIN")) return jdbc.queryForList("SELECT c.id,c.campus_code AS campusCode,c.name,c.address,c.status FROM school_campus c JOIN student_campus_admin a ON a.campus_id=c.id AND a.tenant_id=c.tenant_id WHERE c.tenant_id=? AND a.user_id=? ORDER BY c.name,c.id",caller.tenantId(),caller.userId());
        return jdbc.queryForList("SELECT DISTINCT c.id,c.campus_code AS campusCode,c.name,c.address,c.status FROM school_campus c JOIN student_homeroom_assignment x ON x.class_id IN (SELECT id FROM school_class WHERE campus_id=c.id AND tenant_id=c.tenant_id) AND x.tenant_id=c.tenant_id WHERE c.tenant_id=? AND x.user_id=? AND x.ends_on IS NULL ORDER BY c.name,c.id",caller.tenantId(),caller.userId());
    }

    public Map<String,Object> createCampus(Map<String,Object> input, InternalCallContext caller) {
        requireSuper(caller);
        String code = required(input,"campusCode",32), name = required(input,"name",128);
        jdbc.update("INSERT INTO school_campus(tenant_id,campus_code,name,address) VALUES(?,?,?,?)", caller.tenantId(),code,name,text(input.get("address"),255));
        return jdbc.queryForMap("SELECT id,campus_code AS campusCode,name,address,status FROM school_campus WHERE tenant_id=? AND campus_code=?",caller.tenantId(),code);
    }

    public List<Map<String,Object>> academicYears(InternalCallContext caller) {
        requireSchoolUser(caller);
        return jdbc.queryForList("SELECT id,name,starts_on AS startsOn,ends_on AS endsOn,is_current AS current FROM school_academic_year WHERE tenant_id=? ORDER BY starts_on DESC",caller.tenantId());
    }

    @Transactional
    public Map<String,Object> createAcademicYear(Map<String,Object> input, InternalCallContext caller) {
        requireSuper(caller);
        String name=required(input,"name",32); LocalDate start=LocalDate.parse(required(input,"startsOn",10)); LocalDate end=LocalDate.parse(required(input,"endsOn",10));
        if (!end.isAfter(start)) throw new IllegalArgumentException("学年结束日期必须晚于开始日期");
        boolean current=Boolean.parseBoolean(String.valueOf(input.getOrDefault("current",false)));
        if(current) jdbc.update("UPDATE school_academic_year SET is_current=0 WHERE tenant_id=?",caller.tenantId());
        jdbc.update("INSERT INTO school_academic_year(tenant_id,name,starts_on,ends_on,is_current) VALUES(?,?,?,?,?)",caller.tenantId(),name,start,end,current);
        return jdbc.queryForMap("SELECT id,name,starts_on AS startsOn,ends_on AS endsOn,is_current AS current FROM school_academic_year WHERE tenant_id=? AND name=?",caller.tenantId(),name);
    }

    public List<Map<String,Object>> classes(Long yearId, Long campusId, InternalCallContext caller) {
        requireSchoolUser(caller);
        String scope=isSchoolAdmin(caller)?"":" AND (EXISTS(SELECT 1 FROM student_campus_admin a WHERE a.tenant_id=c.tenant_id AND a.campus_id=c.campus_id AND a.user_id=?) OR EXISTS(SELECT 1 FROM student_homeroom_assignment h WHERE h.tenant_id=c.tenant_id AND h.class_id=c.id AND h.user_id=? AND h.ends_on IS NULL) OR EXISTS(SELECT 1 FROM student_school_class_scope t WHERE t.tenant_id=c.tenant_id AND t.class_id=c.id AND t.user_id=?))";
        if(!scope.isEmpty()) return jdbc.queryForList("SELECT c.id,c.campus_id AS campusId,p.name AS campusName,c.academic_year_id AS academicYearId,y.name AS academicYear,c.grade,c.class_name AS className,c.capacity,c.status FROM school_class c JOIN school_campus p ON p.id=c.campus_id AND p.tenant_id=c.tenant_id JOIN school_academic_year y ON y.id=c.academic_year_id AND y.tenant_id=c.tenant_id WHERE c.tenant_id=? AND (? IS NULL OR c.academic_year_id=?) AND (? IS NULL OR c.campus_id=?)"+scope+" ORDER BY p.name,c.grade,c.class_name",caller.tenantId(),yearId,yearId,campusId,campusId,caller.userId(),caller.userId(),caller.userId());
        return jdbc.queryForList("SELECT c.id,c.campus_id AS campusId,p.name AS campusName,c.academic_year_id AS academicYearId,y.name AS academicYear,c.grade,c.class_name AS className,c.capacity,c.status FROM school_class c JOIN school_campus p ON p.id=c.campus_id AND p.tenant_id=c.tenant_id JOIN school_academic_year y ON y.id=c.academic_year_id AND y.tenant_id=c.tenant_id WHERE c.tenant_id=? AND (? IS NULL OR c.academic_year_id=?) AND (? IS NULL OR c.campus_id=?) ORDER BY p.name,c.grade,c.class_name",caller.tenantId(),yearId,yearId,campusId,campusId);
    }

    public Map<String,Object> createClass(Map<String,Object> input, InternalCallContext caller) {
        requireSchoolAdmin(caller);
        long campus=number(input,"campusId"), year=number(input,"academicYearId"), grade=number(input,"grade");
        if(grade<1||grade>6) throw new IllegalArgumentException("小学年级必须为1至6年级");
        requireCampusAccess(caller,campus,true);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM school_academic_year WHERE id=? AND tenant_id=?",Integer.class,year,caller.tenantId())==0) throw new ManagementNotFoundException();
        String name=required(input,"className",64);
        jdbc.update("INSERT INTO school_class(tenant_id,campus_id,academic_year_id,grade,class_name,capacity) VALUES(?,?,?,?,?,?)",caller.tenantId(),campus,year,grade,name,input.get("capacity"));
        return jdbc.queryForMap("SELECT id,campus_id AS campusId,academic_year_id AS academicYearId,grade,class_name AS className,capacity,status FROM school_class WHERE tenant_id=? AND campus_id=? AND academic_year_id=? AND grade=? AND class_name=?",caller.tenantId(),campus,year,grade,name);
    }

    public PageResult students(Map<String,String> filter,int current,int size,InternalCallContext caller) {
        requireSchoolUser(caller);
        filter=filter==null?Map.of():filter; current=Math.max(1,current);size=Math.max(1,Math.min(200,size));
        Long campus=optionalLong(filter.get("campusId")), year=optionalLong(filter.get("academicYearId")), grade=optionalLong(filter.get("grade")), classId=optionalLong(filter.get("classId"));
        String search=filter.getOrDefault("search","").trim(), status=filter.getOrDefault("status","").trim();
        String sortKey=switch(filter.getOrDefault("sortBy","studentNo")){case "name"->"s.name";case "grade"->"e.grade";case "className"->"c.class_name";case "campusName"->"p.name";case "enrolledOn"->"e.starts_on";default->"s.student_no";};
        String sortDirection="desc".equalsIgnoreCase(filter.get("sortDirection"))?"DESC":"ASC";
        StringBuilder scope=new StringBuilder("s.tenant_id=?"); java.util.ArrayList<Object> args=new java.util.ArrayList<>(); args.add(caller.tenantId());
        List<String> userRoles=roles.findRoleCodes(caller.userId());
        if(!userRoles.contains("R_SUPER") && !userRoles.contains("R_SCHOOL_ADMIN")) {
            if(userRoles.contains("R_CAMPUS_ADMIN")) scope.append(" AND EXISTS(SELECT 1 FROM student_campus_admin ca WHERE ca.tenant_id=s.tenant_id AND ca.user_id=? AND ca.campus_id=e.campus_id)");
            else scope.append(" AND (EXISTS(SELECT 1 FROM student_homeroom_assignment ha WHERE ha.tenant_id=s.tenant_id AND ha.user_id=? AND ha.class_id=e.class_id AND ha.ends_on IS NULL) OR EXISTS(SELECT 1 FROM student_school_class_scope ts WHERE ts.tenant_id=s.tenant_id AND ts.user_id=? AND ts.class_id=e.class_id))");
            args.add(caller.userId());
            if(!userRoles.contains("R_CAMPUS_ADMIN")) args.add(caller.userId());
        }
        if(year==null) { Integer active=jdbc.queryForObject("SELECT COUNT(*) FROM school_academic_year WHERE tenant_id=? AND is_current=1",Integer.class,caller.tenantId()); if(active!=null&&active>0) { scope.append(" AND e.academic_year_id=(SELECT id FROM school_academic_year WHERE tenant_id=? AND is_current=1 LIMIT 1)");args.add(caller.tenantId()); }}
        scope.append(" AND NOT EXISTS(SELECT 1 FROM student_enrollment en2 WHERE en2.tenant_id=e.tenant_id AND en2.student_id=e.student_id AND en2.academic_year_id=e.academic_year_id AND (en2.starts_on>e.starts_on OR (en2.starts_on=e.starts_on AND en2.id>e.id)))");
        if(campus!=null){scope.append(" AND e.campus_id=?");args.add(campus);} if(year!=null){scope.append(" AND e.academic_year_id=?");args.add(year);} if(grade!=null){scope.append(" AND e.grade=?");args.add(grade);} if(classId!=null){scope.append(" AND e.class_id=?");args.add(classId);} if(!status.isBlank()){scope.append(" AND e.status=?");args.add(status);} if(!search.isBlank()){scope.append(" AND (s.student_no LIKE ? OR s.name LIKE ?)");args.add("%"+search+"%");args.add("%"+search+"%");}
        String from=" FROM student s LEFT JOIN student_enrollment e ON e.student_id=s.id AND e.tenant_id=s.tenant_id LEFT JOIN school_campus p ON p.id=e.campus_id AND p.tenant_id=e.tenant_id LEFT JOIN school_class c ON c.id=e.class_id AND c.tenant_id=e.tenant_id LEFT JOIN school_academic_year y ON y.id=e.academic_year_id AND y.tenant_id=e.tenant_id WHERE "+scope;
        Long total=jdbc.queryForObject("SELECT COUNT(DISTINCT s.id)"+from,Long.class,args.toArray());
        args.add(size);args.add((long)(current-1)*size);
        List<Map<String,Object>> records=jdbc.queryForList("SELECT s.id,s.student_no AS studentNo,s.name,s.gender,s.birthday,s.email,s.phone,e.id AS enrollmentId,e.academic_year_id AS academicYearId,y.name AS academicYear,e.campus_id AS campusId,p.name AS campusName,e.grade,e.class_id AS classId,c.class_name AS className,e.status,e.starts_on AS enrolledOn,e.ends_on AS leftOn"+from+" GROUP BY s.id,e.id ORDER BY "+sortKey+" "+sortDirection+",s.id LIMIT ? OFFSET ?",args.toArray());
        return new PageResult(records,current,size,total==null?0:total);
    }

    @Transactional
    public Map<String,Object> createStudent(Map<String,Object> input,InternalCallContext caller) {
        requireSchoolUser(caller); long campus=number(input,"campusId"),year=number(input,"academicYearId"),grade=number(input,"grade");
        requireCampusAccess(caller,campus,true); if(grade<1||grade>6)throw new IllegalArgumentException("小学年级必须为1至6年级");
        String no=required(input,"studentNo",64),name=required(input,"name",128);Long classId=optionalLong(input.get("classId"));
        if(classId!=null && jdbc.queryForObject("SELECT COUNT(*) FROM school_class WHERE id=? AND tenant_id=? AND campus_id=? AND academic_year_id=? AND grade=? AND status='ACTIVE'",Integer.class,classId,caller.tenantId(),campus,year,grade)==0)throw new IllegalArgumentException("班级与校区、学年或年级不匹配");
        String email=text(input.get("email"),128),phone=text(input.get("phone"),32);
        if(email!=null&&!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))throw new IllegalArgumentException("邮箱格式无效");
        if(phone!=null&&!phone.matches("^[0-9+() -]{6,32}$"))throw new IllegalArgumentException("联系电话格式无效");
        Integer duplicate=jdbc.queryForObject("SELECT COUNT(*) FROM student WHERE tenant_id=? AND student_no=?",Integer.class,caller.tenantId(),no);
        if(duplicate!=null&&duplicate>0)throw new ManagementConflictException("学号已存在");
        jdbc.update("INSERT INTO student(tenant_id,student_no,name,gender,birthday,email,phone) VALUES(?,?,?,?,?,?,?)",caller.tenantId(),no,name,text(input.get("gender"),16),input.get("birthday"),email,phone);
        Long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO student_enrollment(tenant_id,student_id,academic_year_id,campus_id,grade,class_id,status,starts_on,changed_by) VALUES(?,?,?,?,?,?,'ENROLLED',?,?)",caller.tenantId(),id,year,campus,grade,classId,LocalDate.now(),caller.userId());
        return jdbc.queryForMap("SELECT id,student_no AS studentNo,name,gender,birthday,email,phone FROM student WHERE id=? AND tenant_id=?",id,caller.tenantId());
    }

    public Map<String,Object> importStudents(List<Map<String,Object>> input,InternalCallContext caller){
        requireSchoolAdmin(caller);if(input==null)throw new IllegalArgumentException("导入数据不能为空");int imported=0;java.util.ArrayList<Map<String,Object>> errors=new java.util.ArrayList<>();
        for(int i=0;i<input.size();i++){Map<String,Object> row=input.get(i);try{
            transactions.execute(status->{
                Map<String,Object> values=new java.util.HashMap<>(row);
                if(values.get("campusId")==null) values.put("campusId",jdbc.queryForObject("SELECT id FROM school_campus WHERE tenant_id=? AND name=?",Long.class,caller.tenantId(),required(values,"campusName",128)));
                if(values.get("academicYearId")==null) values.put("academicYearId",jdbc.queryForObject("SELECT id FROM school_academic_year WHERE tenant_id=? AND name=?",Long.class,caller.tenantId(),required(values,"academicYear",32)));
                if(values.get("grade")==null) values.put("grade",values.get("gradeNo"));
                if(values.get("classId")==null&&values.get("className")!=null&&!values.get("className").toString().isBlank()) values.put("classId",jdbc.queryForObject("SELECT id FROM school_class WHERE tenant_id=? AND campus_id=? AND academic_year_id=? AND grade=? AND class_name=?",Long.class,caller.tenantId(),values.get("campusId"),values.get("academicYearId"),values.get("grade"),values.get("className")));
                createStudent(values,caller);return null;
            });imported++;
        }catch(Exception e){errors.add(Map.of("row",i+2,"studentNo",String.valueOf(row==null?"":row.getOrDefault("studentNo","")),"message",e.getMessage()==null?"导入失败":e.getMessage()));}}
        return Map.of("imported",imported,"total",input.size(),"errors",errors);
    }

    @Transactional
    public void moveStudent(Long studentId,Map<String,Object> input,InternalCallContext caller) {
        requireSchoolAdmin(caller); Long enrollment=optionalLong(input.get("enrollmentId")); if(enrollment==null)throw new IllegalArgumentException("必须指定学年就读记录");
        Map<String,Object> old=jdbc.queryForMap("SELECT campus_id AS campusId,academic_year_id AS yearId,grade,class_id AS classId FROM student_enrollment WHERE id=? AND tenant_id=? AND student_id=? AND status='ENROLLED'",enrollment,caller.tenantId(),studentId);
        requireCampusAccess(caller,((Number)old.get("campusId")).longValue(),true);
        long campus=number(input,"campusId"),grade=number(input,"grade");Long classId=optionalLong(input.get("classId"));requireCampusAccess(caller,campus,true);
        if(grade<1||grade>6)throw new IllegalArgumentException("小学年级必须为1至6年级");
        if(classId!=null&&jdbc.queryForObject("SELECT COUNT(*) FROM school_class WHERE id=? AND tenant_id=? AND campus_id=? AND academic_year_id=? AND grade=?",Integer.class,classId,caller.tenantId(),campus,old.get("yearId"),grade)==0)throw new IllegalArgumentException("目标班级与校区、学年或年级不匹配");
        String reason=text(input.get("reason"),255);LocalDate effective=input.get("effectiveOn")==null?LocalDate.now():LocalDate.parse(input.get("effectiveOn").toString());
        jdbc.update("UPDATE student_enrollment SET status='TRANSFERRED',ends_on=?,change_reason=?,changed_by=? WHERE id=? AND tenant_id=?",effective,reason,caller.userId(),enrollment,caller.tenantId());
        jdbc.update("INSERT INTO student_enrollment(tenant_id,student_id,academic_year_id,campus_id,grade,class_id,status,starts_on,change_reason,changed_by) VALUES(?,?,?,?,?,?,'ENROLLED',?,?,?)",caller.tenantId(),studentId,old.get("yearId"),campus,grade,classId,effective,reason,caller.userId());
    }

    @Transactional
    public void changeEnrollmentStatus(long studentId,Map<String,Object> input,InternalCallContext caller){
        requireSchoolAdmin(caller);Long enrollment=optionalLong(input.get("enrollmentId"));String status=required(input,"status",20);
        if(!List.of("SUSPENDED","ENROLLED","TRANSFERRED","GRADUATED").contains(status))throw new IllegalArgumentException("不支持的学籍状态");
        Map<String,Object> old=jdbc.queryForMap("SELECT academic_year_id AS yearId,campus_id AS campusId,grade,class_id AS classId,status,starts_on AS startsOn FROM student_enrollment WHERE id=? AND tenant_id=? AND student_id=?",enrollment,caller.tenantId(),studentId);requireCampusAccess(caller,((Number)old.get("campusId")).longValue(),true);
        LocalDate effective=input.get("effectiveOn")==null?LocalDate.now():LocalDate.parse(input.get("effectiveOn").toString());
        if(effective.isBefore(((java.sql.Date)old.get("startsOn")).toLocalDate()))throw new IllegalArgumentException("生效日期不能早于当前学籍记录");
        if(status.equals(old.get("status")))throw new IllegalArgumentException("学籍状态没有变化");
        String reason=text(input.get("reason"),255);
        jdbc.update("UPDATE student_enrollment SET ends_on=?,change_reason=?,changed_by=? WHERE id=? AND tenant_id=?",effective,reason,caller.userId(),enrollment,caller.tenantId());
        jdbc.update("INSERT INTO student_enrollment(tenant_id,student_id,academic_year_id,campus_id,grade,class_id,status,starts_on,change_reason,changed_by) VALUES(?,?,?,?,?,?,?,?,?,?)",caller.tenantId(),studentId,old.get("yearId"),old.get("campusId"),old.get("grade"),old.get("classId"),status,effective,reason,caller.userId());
    }

    public void assignHomeroom(long classId,long userId,LocalDate start,InternalCallContext caller) {
        requireSchoolAdmin(caller); Map<String,Object> row=jdbc.queryForMap("SELECT campus_id FROM school_class WHERE id=? AND tenant_id=?",classId,caller.tenantId());requireCampusAccess(caller,((Number)row.get("campus_id")).longValue(),true);
        requireTenantUser(userId,caller);
        assignRole(userId,"R_HOMEROOM");
        jdbc.update("UPDATE student_homeroom_assignment SET ends_on=? WHERE tenant_id=? AND class_id=? AND ends_on IS NULL",start.minusDays(1),caller.tenantId(),classId);
        jdbc.update("INSERT INTO student_homeroom_assignment(tenant_id,class_id,user_id,starts_on) VALUES(?,?,?,?)",caller.tenantId(),classId,userId,start);
    }

    public void assignCampusAdmin(long campusId,long userId,InternalCallContext caller){requireSuper(caller);requireCampusAccess(caller,campusId,true);requireTenantUser(userId,caller);assignRole(userId,"R_CAMPUS_ADMIN");jdbc.update("INSERT IGNORE INTO student_campus_admin(tenant_id,campus_id,user_id) VALUES(?,?,?)",caller.tenantId(),campusId,userId);}

    public void assignTeacher(long classId,long userId,InternalCallContext caller){requireSchoolAdmin(caller);Map<String,Object> row=jdbc.queryForMap("SELECT campus_id FROM school_class WHERE id=? AND tenant_id=?",classId,caller.tenantId());requireCampusAccess(caller,((Number)row.get("campus_id")).longValue(),true);requireTenantUser(userId,caller);assignRole(userId,"R_TEACHER");jdbc.update("INSERT IGNORE INTO student_school_class_scope(tenant_id,class_id,user_id) VALUES(?,?,?)",caller.tenantId(),classId,userId);}

    private void requireSchoolUser(InternalCallContext c){authorization.authorize("student:read",c);if(!roles.findRoleCodes(c.userId()).stream().anyMatch(r->List.of("R_SUPER","R_CAMPUS_ADMIN","R_HOMEROOM","R_TEACHER").contains(r))) throw new ManagementAccessDeniedException();}
    private void requireSchoolAdmin(InternalCallContext c){authorization.authorize("student:write",c);if(!roles.findRoleCodes(c.userId()).stream().anyMatch(r->List.of("R_SUPER","R_SCHOOL_ADMIN","R_CAMPUS_ADMIN").contains(r)))throw new ManagementAccessDeniedException();}
    private void requireSuper(InternalCallContext c){authorization.authorize("student:write",c);if(!isSchoolAdmin(c))throw new ManagementAccessDeniedException();}
    private boolean isSchoolAdmin(InternalCallContext c){List<String> codes=roles.findRoleCodes(c.userId());return codes.contains("R_SUPER")||codes.contains("R_SCHOOL_ADMIN");}
    private void requireCampusAccess(InternalCallContext c,long campus,boolean write){Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM school_campus WHERE tenant_id=? AND id=? AND status='ACTIVE'",Integer.class,c.tenantId(),campus);if(exists==null||exists==0)throw new ManagementNotFoundException();if(isSchoolAdmin(c))return;Integer ok=jdbc.queryForObject("SELECT COUNT(*) FROM student_campus_admin WHERE tenant_id=? AND campus_id=? AND user_id=?",Integer.class,c.tenantId(),campus,c.userId());if(ok==null||ok==0)throw new ManagementAccessDeniedException();}
    private void requireTenantUser(long userId,InternalCallContext c){Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id=? AND tenant_id=? AND status='ENABLED'",Integer.class,userId,c.tenantId());if(count==null||count==0)throw new ManagementNotFoundException();}
    private void assignRole(long userId,String roleCode){jdbc.update("INSERT IGNORE INTO sys_user_role(id,user_id,role_id,create_by,modify_by) SELECT (SELECT COALESCE(MAX(id),0)+1 FROM sys_user_role),?,id,'student-management','student-management' FROM sys_role WHERE role_code=? AND status='ENABLED'",userId,roleCode);}
    private static String required(Map<String,Object> v,String key,int max){String s=text(v.get(key),max);if(s==null||s.isBlank())throw new IllegalArgumentException(key+"不能为空");return s.trim();}
    private static String text(Object v,int max){if(v==null)return null;String s=v.toString().trim();if(s.length()>max)throw new IllegalArgumentException("字段长度超限");return s.isEmpty()?null:s;}
    private static long number(Map<String,Object> v,String key){Long n=optionalLong(v.get(key));if(n==null)throw new IllegalArgumentException(key+"不能为空");return n;}
    private static Long optionalLong(Object v){return v==null||v.toString().isBlank()?null:Long.valueOf(v.toString());}
}
