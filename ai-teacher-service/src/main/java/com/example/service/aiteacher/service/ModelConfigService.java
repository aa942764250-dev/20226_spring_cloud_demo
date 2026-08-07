package com.example.service.aiteacher.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.api.dto.ModelConfigDTO;
import com.example.api.dto.ProviderMeta;
import com.example.api.entity.AiModelConfig;
import com.example.common.result.Result;
import com.example.service.aiteacher.client.OpenAiCompatibleClient;
import com.example.service.aiteacher.config.LlmProviderMeta;
import com.example.service.dao.AiModelConfigDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 模型配置服务：读取当前启用配置、保存、测试连接、暴露提供方列表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigService {

    private final AiModelConfigDao configDao;
    private final OpenAiCompatibleClient openAiClient;

    /** 读取当前配置（优先启用项，否则最新一项）。apiKey 不回显。 */
    public ModelConfigDTO getCurrent() {
        AiModelConfig c = selectActive();
        ModelConfigDTO dto = new ModelConfigDTO();
        if (c == null) {
            dto.setEnabled(0);
            dto.setConfigured(false);
            return dto;
        }
        dto.setProvider(c.getProvider());
        dto.setModelName(c.getModelName());
        dto.setEnabled(c.getEnabled());
        dto.setConfigured(c.getApiKey() != null && !c.getApiKey().isEmpty());
        dto.setApiKey(null);
        return dto;
    }

    /** 保存配置。apiKey 为空时保留原有密钥；启用时禁用其他配置（单激活）。 */
    public void save(ModelConfigDTO req) {
        AiModelConfig c = selectActive();
        if (c == null) {
            c = new AiModelConfig();
            c.setCreatedAt(LocalDateTime.now());
        }
        if (req.getProvider() != null) {
            c.setProvider(req.getProvider());
        }
        if (req.getModelName() != null) {
            c.setModelName(req.getModelName());
        }
        if (req.getApiKey() != null && !req.getApiKey().isEmpty()) {
            c.setApiKey(req.getApiKey());
        }
        Integer enabled = req.getEnabled() != null ? req.getEnabled() : 0;
        c.setEnabled(enabled);
        c.setUpdatedAt(LocalDateTime.now());

        if (enabled == 1) {
            List<AiModelConfig> others = configDao.selectList(new LambdaQueryWrapper<>());
            for (AiModelConfig o : others) {
                if (!Objects.equals(o.getId(), c.getId()) && o.getEnabled() != null && o.getEnabled() == 1) {
                    o.setEnabled(0);
                    o.setUpdatedAt(LocalDateTime.now());
                    configDao.updateById(o);
                }
            }
        }

        if (c.getId() == null) {
            configDao.insert(c);
        } else {
            configDao.updateById(c);
        }
        log.info("AI模型配置已保存 provider={} model={} enabled={}", c.getProvider(), c.getModelName(), enabled);
    }

    /** 测试连接：用给定配置发一个最小请求，成功返回 true。 */
    public Map<String, Object> testConnection(String provider, String modelName, String apiKey) {
        Map<String, Object> res = new HashMap<>();
        String baseUrl = LlmProviderMeta.getBaseUrl(provider);
        if (baseUrl == null) {
            res.put("success", false);
            res.put("message", "未知提供方: " + provider);
            return res;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            res.put("success", false);
            res.put("message", "API Key 不能为空");
            return res;
        }
        String r = openAiClient.generate(baseUrl, modelName, apiKey,
                "你是连接测试助手。", "请只回复两个字：成功");
        if (r != null && !r.isEmpty()) {
            res.put("success", true);
            res.put("message", "连接成功");
        } else {
            res.put("success", false);
            res.put("message", "调用失败，请检查 Key / 模型 / 网络");
        }
        return res;
    }

    /** 暴露提供方列表（含可选模型），供前端下拉联动。 */
    public List<ProviderMeta> providers() {
        return LlmProviderMeta.ALL.stream().map(p -> {
            ProviderMeta pm = new ProviderMeta();
            pm.setProvider(p.getProvider());
            pm.setLabel(p.getLabel());
            pm.setModels(p.getModels().stream()
                    .map(m -> new ProviderMeta.ModelOption(m.getValue(), m.getLabel()))
                    .collect(Collectors.toList()));
            return pm;
        }).collect(Collectors.toList());
    }

    private AiModelConfig selectActive() {
        List<AiModelConfig> all = configDao.selectList(
                new LambdaQueryWrapper<AiModelConfig>().orderByDesc(AiModelConfig::getId));
        if (all.isEmpty()) {
            return null;
        }
        return all.stream()
                .filter(c -> c.getEnabled() != null && c.getEnabled() == 1)
                .findFirst()
                .orElse(all.get(0));
    }
}
