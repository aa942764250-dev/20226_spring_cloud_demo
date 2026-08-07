package com.example.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.api.entity.SelfTestReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SelfTestReportDao extends BaseMapper<SelfTestReport> {
}