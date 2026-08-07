package com.example.service.aiteacher.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.entity.PromptTemplate;
import com.example.common.result.Result;
import com.example.service.dao.PromptTemplateDao;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prompt模板管理接口
 */
@RestController
@RequestMapping("/ai-teacher/prompt")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateDao promptTemplateDao;

    /** 查询所有模板 */
    @GetMapping("/list")
    public Result<List<PromptTemplate>> list(@RequestParam(required = false) String type) {
        LambdaQueryWrapper<PromptTemplate> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(PromptTemplate::getType, type);
        }
        wrapper.orderByAsc(PromptTemplate::getType).orderByDesc(PromptTemplate::getVersion);
        return Result.success(promptTemplateDao.selectList(wrapper));
    }

    /** 获取模板详情 */
    @GetMapping("/{id}")
    public Result<PromptTemplate> detail(@PathVariable Long id) {
        PromptTemplate template = promptTemplateDao.selectById(id);
        if (template == null) {
            return Result.fail(404, "模板不存在");
        }
        return Result.success(template);
    }

    /** 新增/更新模板 */
    @PostMapping
    public Result<Void> save(@RequestBody PromptTemplate template) {
        if (template.getName() == null || template.getName().isEmpty()) {
            return Result.fail("模板名称不能为空");
        }
        if (template.getId() == null) {
            template.setCreatedAt(LocalDateTime.now());
            template.setUpdatedAt(LocalDateTime.now());
            if (template.getEnabled() == null) {
                template.setEnabled(1);
            }
            if (template.getVersion() == null) {
                template.setVersion(1);
            }
            promptTemplateDao.insert(template);
        } else {
            template.setUpdatedAt(LocalDateTime.now());
            promptTemplateDao.updateById(template);
        }
        return Result.success(null);
    }

    /** 删除模板 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        promptTemplateDao.deleteById(id);
        return Result.success(null);
    }
}
