package com.example.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.api.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDao extends BaseMapper<User> {
}