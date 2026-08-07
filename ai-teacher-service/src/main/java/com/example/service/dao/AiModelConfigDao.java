package com.example.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.api.entity.AiModelConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiModelConfigDao extends BaseMapper<AiModelConfig> {
}
