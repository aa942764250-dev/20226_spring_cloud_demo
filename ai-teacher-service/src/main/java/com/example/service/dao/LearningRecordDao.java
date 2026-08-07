package com.example.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.api.entity.LearningRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningRecordDao extends BaseMapper<LearningRecord> {
}
