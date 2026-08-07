package com.example.service.aiteacher.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.api.dto.LearningRecordVO;
import com.example.api.entity.LearningRecord;
import com.example.common.result.Result;

/**
 * 学习记录服务
 */
public interface LearningRecordService {
    /** 分页查询学习记录 */
    Page<LearningRecordVO> listRecords(int page, int size, Long studentId, String startDate, String endDate);
    /** 新增学习记录 */
    Result<Void> addRecord(LearningRecord record);
    /** 更新学习记录 */
    Result<Void> updateRecord(LearningRecord record);
    /** 删除学习记录 */
    Result<Void> deleteRecord(Long recordId);
    /** 获取学生最近N天的学习记录 */
    Page<LearningRecordVO> getRecentRecords(Long studentId, int days);
}
