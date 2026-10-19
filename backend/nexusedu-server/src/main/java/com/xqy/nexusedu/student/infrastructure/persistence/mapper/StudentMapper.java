package com.xqy.nexusedu.student.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xqy.nexusedu.student.infrastructure.persistence.entity.Student;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
