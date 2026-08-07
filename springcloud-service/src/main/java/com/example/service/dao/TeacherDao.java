package com.example.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.api.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TeacherDao extends BaseMapper<Teacher> {
}