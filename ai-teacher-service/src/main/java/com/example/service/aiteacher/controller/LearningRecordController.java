package com.example.service.aiteacher.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.api.dto.LearningRecordVO;
import com.example.api.entity.LearningRecord;
import com.example.common.result.Result;
import com.example.service.aiteacher.service.LearningRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 学习记录接口
 */
@RestController
@RequestMapping("/ai-teacher/record")
@RequiredArgsConstructor
public class LearningRecordController {

    private final LearningRecordService learningRecordService;

    /** 分页查询学习记录 */
    @GetMapping("/list")
    public Result<Page<LearningRecordVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(learningRecordService.listRecords(page, size, studentId, startDate, endDate));
    }

    /** 新增学习记录 */
    @PostMapping
    public Result<Void> add(@RequestBody LearningRecord record) {
        if (record.getStudentId() == null) {
            return Result.fail("学生ID不能为空");
        }
        if (record.getLessonDate() == null) {
            return Result.fail("上课日期不能为空");
        }
        return learningRecordService.addRecord(record);
    }

    /** 更新学习记录 */
    @PutMapping
    public Result<Void> update(@RequestBody LearningRecord record) {
        if (record.getId() == null) {
            return Result.fail("记录ID不能为空");
        }
        return learningRecordService.updateRecord(record);
    }

    /** 删除学习记录 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return learningRecordService.deleteRecord(id);
    }

    /** 获取学生最近N天学习记录 */
    @GetMapping("/recent")
    public Result<Page<LearningRecordVO>> recent(
            @RequestParam Long studentId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(learningRecordService.getRecentRecords(studentId, days));
    }
}
