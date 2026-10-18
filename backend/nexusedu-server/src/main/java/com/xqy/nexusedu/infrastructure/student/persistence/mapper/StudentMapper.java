package com.xqy.nexusedu.infrastructure.student.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.common.entity.student.Student;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
