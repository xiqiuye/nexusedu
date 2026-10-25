package com.xqy.nexusedu.student.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xqy.nexusedu.student.profile.entity.Student;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
    java.util.List<Student> selectCurrentForAi(@Param("tenantId") String tenantId,@Param("userId") Long userId,
          @Param("schoolWide") boolean schoolWide,@Param("name") String name,@Param("gender") String gender,
          @Param("studentNo") String studentNo,@Param("limit") int limit);
}
