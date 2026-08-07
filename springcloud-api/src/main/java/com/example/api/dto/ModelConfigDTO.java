package com.example.api.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * AI 模型配置 DTO。
 * apiKey 仅用于保存入参；查询返回时恒为 null（用 configured 标识是否已配置）。
 */
@Data
public class ModelConfigDTO implements Serializable {
    /** 提供方：kimi / deepseek / siliconflow */
    private String provider;
    /** 模型名称 */
    private String modelName;
    /** 是否启用 0/1 */
    private Integer enabled;
    /** 是否已配置密钥（不返回明文） */
    private Boolean configured;
    /** 保存入参用；返回时恒为 null */
    private String apiKey;
}
