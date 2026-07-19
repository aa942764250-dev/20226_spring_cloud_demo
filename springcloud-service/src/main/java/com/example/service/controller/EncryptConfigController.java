package com.example.service.controller;

import com.example.api.dto.EncryptTableConfigDTO;
import com.example.common.result.Result;
import com.example.service.service.EncryptConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 加密配置管理REST接口。
 * <p>
 * 提供加密配置的增删改查能力，配置以"表+多字段"为基本单元，
 * 每个字段可独立指定加密算法和执行顺序。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/encrypt/config")
@RequiredArgsConstructor
public class EncryptConfigController {

    private final EncryptConfigService encryptConfigService;

    /**
     * 创建加密配置。
     *
     * @param config 加密配置（含表名和字段列表）
     * @return 新创建的配置ID
     */
    @PostMapping
    public Result<Long> createConfig(@RequestBody EncryptTableConfigDTO config) {
        return Result.success(encryptConfigService.createConfig(config));
    }

    /**
     * 更新加密配置。
     *
     * @param config 新的加密配置
     * @param id     配置ID
     * @return 空响应
     */
    @PutMapping("/{id}")
    public Result<Void> updateConfig(@PathVariable Long id, @RequestBody EncryptTableConfigDTO config) {
        encryptConfigService.updateConfig(id, config);
        return Result.success(null);
    }

    /**
     * 删除加密配置（级联删除字段配置）。
     *
     * @param id 配置ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        encryptConfigService.deleteConfig(id);
        return Result.success(null);
    }

    /**
     * 查询所有加密配置。
     *
     * @return 加密配置列表（含字段配置）
     */
    @GetMapping
    public Result<List<EncryptTableConfigDTO>> listConfigs() {
        return Result.success(encryptConfigService.listConfigs());
    }
}
