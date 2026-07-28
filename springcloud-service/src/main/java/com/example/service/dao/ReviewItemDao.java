package com.example.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.api.entity.ReviewItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewItemDao extends BaseMapper<ReviewItem> {
}