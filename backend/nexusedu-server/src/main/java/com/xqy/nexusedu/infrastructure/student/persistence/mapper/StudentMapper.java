package com.xqy.nexusedu.infrastructure.student.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.infrastructure.student.persistence.entity.Student;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
