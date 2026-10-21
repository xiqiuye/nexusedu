package com.xqy.nexusedu.student.profile.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import com.xqy.nexusedu.student.profile.infrastructure.persistence.entity.Student;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
    @Select("""
      SELECT DISTINCT s.* FROM student s
      JOIN student_enrollment e ON e.student_id=s.id AND e.tenant_id=s.tenant_id AND e.status='ENROLLED'
      JOIN school_academic_year y ON y.id=e.academic_year_id AND y.tenant_id=e.tenant_id AND y.is_current=1
      WHERE s.tenant_id=#{tenantId}
        AND (#{name} IS NULL OR s.name LIKE CONCAT('%',#{name},'%'))
        AND (#{gender} IS NULL OR s.gender=#{gender})
        AND (#{studentNo} IS NULL OR s.student_no=#{studentNo})
        AND (#{schoolWide}=1 OR EXISTS(SELECT 1 FROM student_campus_admin ca WHERE ca.tenant_id=s.tenant_id AND ca.campus_id=e.campus_id AND ca.user_id=#{userId})
          OR EXISTS(SELECT 1 FROM student_homeroom_assignment ha WHERE ha.tenant_id=s.tenant_id AND ha.class_id=e.class_id AND ha.user_id=#{userId} AND ha.ends_on IS NULL)
          OR EXISTS(SELECT 1 FROM student_school_class_scope ts WHERE ts.tenant_id=s.tenant_id AND ts.class_id=e.class_id AND ts.user_id=#{userId}))
      ORDER BY s.student_no LIMIT #{limit}
      """)
    java.util.List<Student> selectCurrentForAi(@Param("tenantId") String tenantId,@Param("userId") Long userId,
          @Param("schoolWide") boolean schoolWide,@Param("name") String name,@Param("gender") String gender,
          @Param("studentNo") String studentNo,@Param("limit") int limit);
}
