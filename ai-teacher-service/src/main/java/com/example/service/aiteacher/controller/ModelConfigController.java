package com.example.service.aiteacher.controller;

import com.example.api.dto.ModelConfigDTO;
import com.example.api.dto.ProviderMeta;
import com.example.common.result.Result;
import com.example.service.aiteacher.service.ModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 模型配置接口
 */
@RestController
@RequestMapping("/ai-teacher/model-config")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService service;

    /** 获取当前配置（apiKey 不回显） */
    @GetMapping
    public Result<ModelConfigDTO> get() {
        return Result.success(service.getCurrent());
    }

    /** 保存配置 */
    @PutMapping
    public Result<Void> save(@RequestBody ModelConfigDTO dto) {
        service.save(dto);
        return Result.success(null);
    }

    /** 测试连接（provider / modelName / apiKey 入参） */
    @PostMapping("/test")
    public Result<Map<String, Object>> test(@RequestBody ModelConfigDTO dto) {
        Map<String, Object> r = service.testConnection(dto.getProvider(), dto.getModelName(), dto.getApiKey());
        return Boolean.TRUE.equals(r.get("success")) ? Result.success(r) : Result.fail(r.get("message").toString());
    }

    /** 提供方与模型列表（前端下拉联动） */
    @GetMapping("/providers")
    public Result<List<ProviderMeta>> providers() {
        return Result.success(service.providers());
    }
}
