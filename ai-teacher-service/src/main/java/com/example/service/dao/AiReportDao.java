package com.example.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.api.entity.AiReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AiReportDao extends BaseMapper<AiReport> {

    /**
     * 扫描可认领的报告任务（DB 任务队列）：
     *  - status = 4：待生成（HTTP 请求已写入，但还未被任何实例认领）
     *  - status = 5 且 updated_at 已超过租约：卡死/崩溃恢复的"已认领但未完成"任务
     * 多实例安全说明：下方 claimPending / reclaimStale 的 UPDATE 命中行时，MySQL 会对该行加 X 锁，
     * 两个实例并发执行同一 UPDATE 时只有一个能命中（另一个 WHERE 条件不匹配 → 影响 0 行），天然避免重复认领。
     */
    @Select("SELECT id FROM ai_report WHERE status = 4 " +
            "OR (status = 5 AND updated_at < DATE_ADD(NOW(), INTERVAL #{leaseMinutes} MINUTE)) " +
            "ORDER BY created_at ASC LIMIT #{limit}")
    List<Long> selectClaimableIds(@Param("limit") int limit, @Param("leaseMinutes") int leaseMinutes);

    /**
     * 认领"待生成"任务：仅当 status = 4 时置为 5（已认领/执行中）。
     * 返回受影响行数，0 表示已被其它实例抢先认领，本实例应跳过。
     */
    @Update("UPDATE ai_report SET status = 5, updated_at = NOW() WHERE id = #{id} AND status = 4")
    int claimPending(@Param("id") Long id);

    /**
     * 超时认领"卡死"任务：status = 5 且已超过租约时间，重置 updated_at 重新执行。
     * 返回受影响行数，0 表示未超时或已被其它实例认领。
     */
    @Update("UPDATE ai_report SET status = 5, updated_at = NOW() WHERE id = #{id} AND status = 5 " +
            "AND updated_at < DATE_ADD(NOW(), INTERVAL #{leaseMinutes} MINUTE)")
    int reclaimStale(@Param("id") Long id, @Param("leaseMinutes") int leaseMinutes);
}
